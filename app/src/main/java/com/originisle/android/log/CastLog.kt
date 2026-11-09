package com.originisle.android.log

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * A persistent ring buffer of what the notification listener decided for each notification —
 * cast (which card type) or skipped (with the reason).
 *
 * Backs the in-app "Log" screen so "why didn't this cast?" is answerable at a glance.
 * Now saved to local storage (`cast_logs.json`) so logs survive process death and Vivo background kills.
 */
object CastLog {

    /** @param cast true = it was cast to the island, false = skipped. */
    data class Entry(
        val time: Long,
        val app: String,
        val title: String,
        val outcome: String,
        val cast: Boolean,
    )

    private const val MAX = 200
    private const val FILE_NAME = "cast_logs.json"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var isLoaded = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Newest first. A Compose snapshot list, so the Log screen updates live. */
    val entries = mutableStateListOf<Entry>()

    /** Ensure logs are loaded from persistent storage into [entries]. */
    fun ensureLoaded(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        if (!isLoaded) {
            synchronized(this) {
                if (!isLoaded) {
                    loadFromDisk(context.applicationContext)
                    isLoaded = true
                }
            }
        }
    }

    private fun loadFromDisk(context: Context) {
        runCatching {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return
            val jsonStr = file.readText()
            if (jsonStr.isBlank()) return
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Entry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Entry(
                        time = obj.optLong("time"),
                        app = obj.optString("app"),
                        title = obj.optString("title"),
                        outcome = obj.optString("outcome"),
                        cast = obj.optBoolean("cast"),
                    )
                )
            }
            if (list.isNotEmpty()) {
                val combined = (entries.toList() + list).distinctBy { "${it.time}_${it.app}_${it.title}" }
                    .sortedByDescending { it.time }
                    .take(MAX)
                entries.clear()
                entries.addAll(combined)
            }
        }
    }

    private val otpRedactRegex = Regex("""(?i)\b(mã\s*otp|otp|code|mã\s*xác\s*thực|mã\s*xác\s*minh)\s*[:：\-]?\s*\d{4,8}\b""")
    private val accountRedactRegex = Regex("""\b(\d{3})\d{4,10}(\d{2})\b""")

    private fun redactSensitive(input: String): String {
        return input
            .replace(otpRedactRegex, "$1: ••••")
            .replace(accountRedactRegex, "$1••••$2")
    }

    private fun persist() {
        val ctx = appContext ?: return
        val snapshot = synchronized(this) { entries.toList() }
        scope.launch {
            runCatching {
                val arr = JSONArray()
                for (e in snapshot) {
                    val obj = JSONObject().apply {
                        put("time", e.time)
                        put("app", e.app)
                        put("title", redactSensitive(e.title))
                        put("outcome", redactSensitive(e.outcome))
                        put("cast", e.cast)
                    }
                    arr.put(obj)
                }
                val file = File(ctx.filesDir, FILE_NAME)
                val tempFile = File(ctx.filesDir, "$FILE_NAME.tmp")
                tempFile.writeText(arr.toString())
                tempFile.renameTo(file)
            }
        }
    }

    fun add(app: String, title: String, outcome: String, cast: Boolean, context: Context? = null) {
        if (context != null && appContext == null) {
            appContext = context.applicationContext
            if (!isLoaded) {
                loadFromDisk(context.applicationContext)
                isLoaded = true
            }
        }
        val entry = Entry(System.currentTimeMillis(), app, title, outcome, cast)
        entries.add(0, entry)
        while (entries.size > MAX) entries.removeAt(entries.size - 1)
        persist()
    }

    fun clear(context: Context? = null) {
        entries.clear()
        val ctx = context?.applicationContext ?: appContext
        if (ctx != null) {
            scope.launch {
                runCatching {
                    val file = File(ctx.filesDir, FILE_NAME)
                    if (file.exists()) file.delete()
                    val tempFile = File(ctx.filesDir, "$FILE_NAME.tmp")
                    if (tempFile.exists()) tempFile.delete()
                }
            }
        }
    }
}
