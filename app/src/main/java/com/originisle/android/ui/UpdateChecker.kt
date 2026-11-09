package com.originisle.android.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/**
 * Quản lý kiểm tra phiên bản mới từ GitHub Releases, tải tệp APK và khởi chạy cài đặt tự động
 * thông qua FileProvider.
 */
object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/trungdzid3/originislevn/releases/latest"

    data class ReleaseInfo(
        val versionName: String,
        val title: String,
        val changelog: String,
        val apkUrl: String,
        val apkName: String,
        val sizeBytes: Long,
    )

    sealed class Result {
        data class UpToDate(val current: String) : Result()
        data class UpdateAvailable(val release: ReleaseInfo) : Result() {
            // Getter tương thích ngược cho các nơi truy cập phiên bản cũ
            val latest: String get() = release.versionName
            val url: String get() = release.apkUrl
        }
        data class Error(val message: String) : Result()
    }

    /**
     * Làm sạch tên tệp APK, chống tấn công Path Traversal.
     */
    fun sanitizeApkFileName(rawName: String): String {
        val baseName = File(rawName).name
        var sanitized = baseName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trim('_', '.')
        if (sanitized.isEmpty()) {
            sanitized = "update.apk"
        }
        if (!sanitized.endsWith(".apk", ignoreCase = true)) {
            sanitized += ".apk"
        }
        return sanitized
    }

    /**
     * Kiểm tra URL chỉ cho phép giao thức HTTPS và các tên miền chính thức của GitHub.
     */
    fun isAllowedGithubUrl(urlStr: String): Boolean {
        return runCatching {
            val url = URL(urlStr)
            if (!url.protocol.equals("https", ignoreCase = true)) return false
            val host = url.host?.lowercase(Locale.ROOT) ?: return false
            host == "github.com" ||
                host == "api.github.com" ||
                host.endsWith(".github.com") ||
                host.endsWith(".githubusercontent.com")
        }.getOrDefault(false)
    }

    /**
     * Xóa các tệp .apk và .download cũ trong thư mục cache updates.
     */
    fun cleanupOldApks(context: Context) {
        runCatching {
            val updateDir = File(context.cacheDir, "updates")
            if (updateDir.exists() && updateDir.isDirectory) {
                updateDir.listFiles()?.forEach { file ->
                    if (file.isFile && (
                            file.name.endsWith(".apk", ignoreCase = true) ||
                                file.name.endsWith(".download", ignoreCase = true)
                            )
                    ) {
                        file.delete()
                    }
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "Không thể dọn dẹp APK cũ: ${e.message}")
        }
    }

    /**
     * Xác thực tên gói và chữ ký bảo mật của tệp APK tải về khớp với ứng dụng đang cài đặt.
     */
    fun verifyDownloadedApk(context: Context, apkFile: File): Boolean {
        return runCatching {
            if (!apkFile.exists() || !apkFile.isFile) return false
            val pm = context.packageManager
            val archiveInfo = pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_SIGNING_CERTIFICATES,
            ) ?: return false

            if (archiveInfo.packageName != context.packageName) {
                Log.e(TAG, "Tên gói APK không khớp: ${archiveInfo.packageName} != ${context.packageName}")
                return false
            }

            val archiveSigners = archiveInfo.signingInfo?.apkContentsSigners
                ?: archiveInfo.signingInfo?.signingCertificateHistory

            val installedInfo = pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            val installedSigners = installedInfo.signingInfo?.apkContentsSigners
                ?: installedInfo.signingInfo?.signingCertificateHistory

            if (archiveSigners.isNullOrEmpty() || installedSigners.isNullOrEmpty()) {
                Log.e(TAG, "Không tìm thấy thông tin chữ ký của APK hoặc ứng dụng hiện tại")
                return false
            }

            archiveSigners.any { archiveCert ->
                val archiveBytes = archiveCert.toByteArray()
                installedSigners.any { installedCert ->
                    archiveBytes.contentEquals(installedCert.toByteArray())
                }
            }
        }.getOrElse { e ->
            Log.e(TAG, "Lỗi khi xác thực chữ ký APK: ${e.message}", e)
            false
        }
    }

    suspend fun check(context: Context): Result = withContext(Dispatchers.IO) {
        val current = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: return@withContext Result.Error("Không rõ phiên bản đã cài đặt")

        try {
            val conn = (URL(LATEST_RELEASE_URL).openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "OriginIsle-Updater")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.Error("Lỗi kết nối GitHub (Mã HTTP: $responseCode)")
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val rawTag = json.optString("tag_name", "")
            val tagName = rawTag.removePrefix("v").trim()
            val title = json.optString("name").ifBlank { rawTag }
            val changelog = json.optString("body", "Không có thông tin thay đổi.")
            val htmlUrl = json.optString("html_url", "")

            val assets = json.optJSONArray("assets")
            var bestApkUrl = ""
            var bestApkName = ""
            var bestApkSize = 0L

            if (assets != null && assets.length() > 0) {
                var fallbackApkUrl = ""
                var fallbackApkName = ""
                var fallbackApkSize = 0L

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val aName = asset.optString("name", "")
                    val aUrl = asset.optString("browser_download_url", "")
                    val aSize = asset.optLong("size", 0L)

                    if (aName.endsWith(".apk", ignoreCase = true)) {
                        if (aName.contains("OriginIsle-VN", ignoreCase = true) ||
                            aName.contains("OriginIsle", ignoreCase = true)
                        ) {
                            bestApkName = aName
                            bestApkUrl = aUrl
                            bestApkSize = aSize
                            break
                        } else if (fallbackApkUrl.isEmpty()) {
                            fallbackApkName = aName
                            fallbackApkUrl = aUrl
                            fallbackApkSize = aSize
                        }
                    }
                }

                if (bestApkUrl.isEmpty() && fallbackApkUrl.isNotEmpty()) {
                    bestApkName = fallbackApkName
                    bestApkUrl = fallbackApkUrl
                    bestApkSize = fallbackApkSize
                }
            }

            if (bestApkUrl.isEmpty()) {
                bestApkUrl = htmlUrl
                bestApkName = "OriginIsle-VN-v$tagName.apk"
            }

            val release = ReleaseInfo(
                versionName = tagName,
                title = title,
                changelog = changelog,
                apkUrl = bestApkUrl,
                apkName = sanitizeApkFileName(bestApkName),
                sizeBytes = bestApkSize,
            )

            if (isNewer(tagName, current)) {
                Result.UpdateAvailable(release)
            } else {
                cleanupOldApks(context)
                Result.UpToDate(current)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Kiểm tra cập nhật thất bại: ${e.message}")
            Result.Error("Không thể kiểm tra bản cập nhật: ${e.localizedMessage ?: "Lỗi kết nối"}")
        }
    }

    /**
     * Tải tệp APK từ release.apkUrl vào thư mục cache updates. Hỗ trợ redirect HTTP 30x (GitHub CDN)
     * và gọi callback onProgress liên tục.
     */
    suspend fun downloadApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (progress: Int, downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) {
                updateDir.mkdirs()
            }

            val safeName = sanitizeApkFileName(release.apkName)
            val destFile = File(updateDir, safeName)
            val tempFile = File(updateDir, "$safeName.download")

            val canonicalDirPrefix = updateDir.canonicalPath + File.separator
            if (!destFile.canonicalPath.startsWith(canonicalDirPrefix) ||
                !tempFile.canonicalPath.startsWith(canonicalDirPrefix)
            ) {
                Log.e(TAG, "Phát hiện đường dẫn tệp APK không hợp lệ: $safeName")
                return@withContext null
            }

            if (tempFile.exists()) {
                tempFile.delete()
            }

            var currentUrl = release.apkUrl
            var connection: HttpsURLConnection? = null
            var redirects = 0
            val maxRedirects = 6

            while (redirects < maxRedirects) {
                if (!isAllowedGithubUrl(currentUrl)) {
                    Log.e(TAG, "URL tải xuống không thuộc danh sách tin cậy: $currentUrl")
                    return@withContext null
                }
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpsURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "OriginIsle-Updater")
                    setRequestProperty("Accept", "*/*")
                }
                val status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrEmpty()) {
                        val resolvedUrl = runCatching { URL(url, location).toString() }.getOrDefault(location)
                        if (!isAllowedGithubUrl(resolvedUrl)) {
                            Log.e(TAG, "URL chuyển hướng không thuộc danh sách tin cậy: $resolvedUrl")
                            return@withContext null
                        }
                        currentUrl = resolvedUrl
                        redirects++
                        continue
                    }
                }
                connection = conn
                break
            }

            val conn = connection ?: return@withContext null
            if (conn.responseCode !in 200..299) {
                Log.e(TAG, "Tải APK thất bại, mã HTTP: ${conn.responseCode}")
                conn.disconnect()
                return@withContext null
            }

            val totalBytes = if (conn.contentLengthLong > 0) {
                conn.contentLengthLong
            } else if (release.sizeBytes > 0) {
                release.sizeBytes
            } else {
                -1L
            }

            var downloadedBytes = 0L
            val buffer = ByteArray(16 * 1024)

            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    var lastProgress = -1
                    var lastTime = 0L
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }

                        val now = System.currentTimeMillis()
                        if (progress != lastProgress || now - lastTime >= 50) {
                            lastProgress = progress
                            lastTime = now
                            withContext(Dispatchers.Main) {
                                onProgress(progress, downloadedBytes, totalBytes)
                            }
                        }
                    }
                    output.flush()
                }
            }
            conn.disconnect()

            withContext(Dispatchers.Main) {
                onProgress(100, downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes)
            }

            if (destFile.exists()) {
                destFile.delete()
            }
            if (tempFile.renameTo(destFile)) {
                destFile
            } else {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
                destFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong quá trình tải APK: ${e.message}", e)
            null
        }
    }

    /**
     * Khởi chạy Intent cài đặt APK qua FileProvider sau khi đã xác thực chữ ký bảo mật.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!verifyDownloadedApk(context, apkFile)) {
                runCatching { apkFile.delete() }
                runCatching {
                    val showToast = Runnable {
                        Toast.makeText(
                            context.applicationContext,
                            "Tệp APK không hợp lệ hoặc khác chữ ký bảo mật",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        showToast.run()
                    } else {
                        Handler(Looper.getMainLooper()).post(showToast)
                    }
                }
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Không thể khởi chạy cài đặt APK: ${e.message}", e)
        }
    }

    /** "1.2.10" > "1.2.9" — so sánh các phân đoạn số cách nhau bằng dấu chấm. */
    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    fun openRelease(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e(TAG, "Không thể mở liên kết phát hành: ${e.message}", e)
        }
    }
}
