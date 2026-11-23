package com.originisle.android.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.Process
import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object ShizukuKeepAliveHelper {
    const val STATUS_NOT_INSTALLED = 0
    const val STATUS_NOT_RUNNING = 1
    const val STATUS_NO_PERMISSION = 2
    const val STATUS_READY = 3

    private const val PREFS_NAME = "experimental_prefs"
    private const val KEY_AUTO_KEEPALIVE = "shizuku_auto_keepalive"
    private const val KEY_LAST_SUMMARY = "shizuku_last_summary"
    private const val KEY_LAST_APPLIED_MS = "shizuku_last_applied_ms"

    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE_SHIZUKU = 9201
    private const val PROCESS_TIMEOUT_SECONDS = 10L
    private const val AUTO_REAPPLY_INTERVAL_MS = 6L * 60L * 60L * 1000L

    private val initialized = AtomicBoolean(false)
    private val applying = AtomicBoolean(false)
    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    @Volatile
    private var pendingPermissionCallback: ((Boolean) -> Unit)? = null

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != REQUEST_CODE_SHIZUKU) return@OnRequestPermissionResultListener
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            val callback = pendingPermissionCallback
            pendingPermissionCallback = null
            if (callback != null) {
                mainHandler.post { callback(granted) }
            }
        }

    fun init(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        // Keep application context reference implicit; listener registration is process-wide.
        context.applicationContext
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (_: Throwable) {
            initialized.set(false)
        }
    }

    fun getState(context: Context): Int {
        if (!isShizukuInstalled(context)) return STATUS_NOT_INSTALLED
        val binderAlive = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
        if (!binderAlive) return STATUS_NOT_RUNNING

        val granted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
        return if (granted) STATUS_READY else STATUS_NO_PERMISSION
    }

    fun getStateLabel(context: Context): String = when (getState(context)) {
        STATUS_NOT_INSTALLED -> "Chưa cài Shizuku"
        STATUS_NOT_RUNNING -> "Shizuku đang tắt"
        STATUS_NO_PERMISSION -> "Chưa cấp quyền Shizuku"
        STATUS_READY -> "Shizuku sẵn sàng"
        else -> "Chưa cài Shizuku"
    }

    fun requestPermission(onResult: (granted: Boolean) -> Unit) {
        val binderAlive = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
        if (!binderAlive) {
            mainHandler.post { onResult(false) }
            return
        }

        val alreadyGranted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
        if (alreadyGranted) {
            mainHandler.post { onResult(true) }
            return
        }

        val preV11 = try {
            Shizuku.isPreV11()
        } catch (_: Throwable) {
            true
        }
        if (preV11) {
            mainHandler.post { onResult(false) }
            return
        }

        pendingPermissionCallback = onResult
        try {
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        } catch (_: Throwable) {
            pendingPermissionCallback = null
            mainHandler.post { onResult(false) }
        }
    }

    fun buildSafeKeepAliveScript(pkg: String, uid: Int, userId: Int): String {
        val safePkg = pkg.filter { it.isLetterOrDigit() || it == '.' || it == '_' }
        require(safePkg.isNotEmpty()) { "Invalid package name" }
        return """
            PKG='$safePkg'
            U='$userId'
            UID='$uid'
            (cmd deviceidle whitelist +"${'$'}PKG" || dumpsys deviceidle whitelist +"${'$'}PKG") >/dev/null 2>&1 || true
            am set-standby-bucket --user "${'$'}U" "${'$'}PKG" active >/dev/null 2>&1 || true
            for OP in RUN_IN_BACKGROUND RUN_ANY_IN_BACKGROUND START_FOREGROUND WAKE_LOCK POST_NOTIFICATION; do
              cmd appops set --user "${'$'}U" "${'$'}PKG" "${'$'}OP" allow >/dev/null 2>&1 || true
            done
            cmd notification allow_listener "${'$'}PKG/com.originisle.android.service.NotificationCastListener" >/dev/null 2>&1 || true
            BUCKET=$(am get-standby-bucket --user "${'$'}U" "${'$'}PKG" 2>/dev/null | tr -d '\r\n')
            WL=$( (cmd deviceidle whitelist || dumpsys deviceidle whitelist) 2>/dev/null | grep -F "${'$'}PKG" | head -n 1 | tr -d '\r\n')
            [ -n "${'$'}WL" ] && WL=1 || WL=0
            echo "OK|BUCKET=${'$'}{BUCKET:-unknown}|WL=${'$'}WL"
        """.trimIndent()
    }

    fun applyKeepAliveAsync(
        context: Context,
        onResult: (success: Boolean, message: String) -> Unit,
    ) {
        val appContext = context.applicationContext
        if (getState(appContext) != STATUS_READY) {
            val label = getStateLabel(appContext)
            mainHandler.post { onResult(false, label) }
            return
        }
        if (!applying.compareAndSet(false, true)) {
            mainHandler.post { onResult(false, "Đang áp dụng Shizuku keep-alive") }
            return
        }

        Thread({
            var process: java.lang.Process? = null
            try {
                val pkg = appContext.packageName
                val uid = Process.myUid()
                val userId = uid / 100_000
                val script = buildSafeKeepAliveScript(pkg, uid, userId)

                process = newShizukuProcess(arrayOf("sh", "-c", script))
                val finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    val timeoutMsg = "Quá thời gian thực thi Shizuku (${PROCESS_TIMEOUT_SECONDS}s)"
                    saveSummary(appContext, timeoutMsg, updateTimestamp = false)
                    mainHandler.post { onResult(false, timeoutMsg) }
                    return@Thread
                }

                val stdout = process.inputStream.bufferedReader().use { it.readText().trim() }
                val stderr = process.errorStream.bufferedReader().use { it.readText().trim() }
                val exitCode = process.exitValue()
                val success = exitCode == 0 && stdout.contains("OK|")
                val summary = when {
                    success -> stdout.lineSequence().lastOrNull { it.startsWith("OK|") } ?: stdout
                    stderr.isNotEmpty() -> "Lỗi ($exitCode): $stderr"
                    stdout.isNotEmpty() -> "Lỗi ($exitCode): $stdout"
                    else -> "Lỗi thực thi Shizuku (mã $exitCode)"
                }

                saveSummary(appContext, summary, updateTimestamp = success)
                mainHandler.post { onResult(success, summary) }
            } catch (t: Throwable) {
                val errMsg = "Lỗi Shizuku: ${t.message ?: t.javaClass.simpleName}"
                saveSummary(appContext, errMsg, updateTimestamp = false)
                mainHandler.post { onResult(false, errMsg) }
            } finally {
                try {
                    process?.destroy()
                } catch (_: Throwable) {
                }
                applying.set(false)
            }
        }, "shizuku-keepalive").start()
    }

    fun ensureAutoAppliedAsync(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_AUTO_KEEPALIVE, false)) return

        val lastAppliedMs = prefs.getLong(KEY_LAST_APPLIED_MS, 0L)
        if (System.currentTimeMillis() - lastAppliedMs < AUTO_REAPPLY_INTERVAL_MS) return

        if (getState(appContext) != STATUS_READY) return
        applyKeepAliveAsync(appContext) { _, _ -> }
    }

    fun getLastSummary(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SUMMARY, "") ?: ""
    }

    private fun saveSummary(context: Context, summary: String, updateTimestamp: Boolean) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_SUMMARY, summary)
        if (updateTimestamp) {
            editor.putLong(KEY_LAST_APPLIED_MS, System.currentTimeMillis())
        }
        editor.apply()
    }

    private fun newShizukuProcess(cmd: Array<String>): java.lang.Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        ).apply { isAccessible = true }
        return method.invoke(null, cmd, null, null) as java.lang.Process
    }

    private fun isShizukuInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(
            SHIZUKU_PACKAGE,
            PackageManager.PackageInfoFlags.of(0L),
        )
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: Throwable) {
        false
    }
}
