package com.originisle.android.download

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.util.regex.Pattern

object DownloadDetector {

    private val DOWNLOAD_PACKAGES = setOf(
        "com.android.vending",               // Google Play Store
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.android.providers.downloads",   // Android Download Manager
        "com.android.providers.downloads.ui",
        // Chrome mọi biến thể
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.canary",
        "com.chrome.dev",
        // Edge mọi biến thể
        "com.microsoft.emmx",
        "com.microsoft.emmx.canary",
        "com.microsoft.emmx.dev",
        "com.microsoft.emmx.beta",
        // Trình duyệt khác
        "com.vivo.browser",
        "com.brave.browser",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.coccoc.trinhduyet",
    )

    private val PERCENT_PATTERN = Pattern.compile("(\\d{1,3})\\s*%")
    private val SIZE_PATTERN = Pattern.compile(
        "(\\d+(?:[.,]\\d+)?)\\s*(?:MB|GB|KB|kB|M|G|K)?\\s*/\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:MB|GB|KB|kB|M|G|K)",
        Pattern.CASE_INSENSITIVE
    )
    private val SPEED_PATTERN = Pattern.compile(
        "(\\d+(?:[.,]\\d+)?\\s*(?:KB/s|MB/s|GB/s|kB/s|kb/s|mb/s))",
        Pattern.CASE_INSENSITIVE
    )

    fun isDownload(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val extras = n.extras
        val pkg = sbn.packageName

        // 1. Kiểm tra progress bar trực tiếp từ hệ thống
        val max = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false)
        if (max > 0 || indeterminate) return true

        // 2. Kiểm tra category chuẩn
        if (n.category == Notification.CATEGORY_PROGRESS) return true

        // 3. Kiểm tra nội dung text
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val allText = "$title $text $bigText $subText".lowercase()

        val hasDownloadKeywords = allText.contains("đang tải") ||
            allText.contains("tải xuống") ||
            allText.contains("tải về") ||
            allText.contains("downloading") ||
            allText.contains("download") ||
            allText.contains("đang cài đặt") ||
            allText.contains("installing") ||
            allText.contains("đang chờ") ||
            allText.contains("pending") ||
            allText.contains("đã tải xong") ||
            allText.contains("đã cài đặt") ||
            allText.contains("installed")

        if (hasDownloadKeywords) return true

        // 4. Nếu từ gói tải xuống/trình duyệt và có dấu hiệu tiến trình (% hoặc size hoặc tốc độ)
        val isDownloadPackage = pkg in DOWNLOAD_PACKAGES ||
            pkg.startsWith("com.microsoft.emmx") ||
            pkg.startsWith("com.android.chrome") ||
            pkg.startsWith("com.chrome") ||
            pkg == "com.android.vending" ||
            pkg.startsWith("com.android.providers.downloads")

        if (isDownloadPackage) {
            if (PERCENT_PATTERN.matcher(allText).find()) return true
            if (SIZE_PATTERN.matcher(allText).find()) return true
            if (SPEED_PATTERN.matcher(allText).find()) return true
        }

        return false
    }

    fun parse(sbn: StatusBarNotification): DownloadInfo? {
        val n = sbn.notification
        val extras = n.extras
        val pkg = sbn.packageName

        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()

        val fullText = "$title $text $bigText $subText".trim()
        val lower = fullText.lowercase()

        // Trạng thái hoàn thành
        val isFinished = lower.contains("đã cài đặt") ||
            lower.contains("installed") ||
            lower.contains("hoàn tất") ||
            lower.contains("complete") ||
            lower.contains("thành công") ||
            lower.contains("tải xong")

        // Trích xuất tốc độ tải (speedText) từ subText/text/bigText nếu chưa hoàn tất
        var speedText: String? = null
        if (!isFinished) {
            val subMatcher = SPEED_PATTERN.matcher(subText)
            if (subMatcher.find()) {
                speedText = subMatcher.group(1)?.trim()
            } else {
                val textMatcher = SPEED_PATTERN.matcher(text)
                if (textMatcher.find()) {
                    speedText = textMatcher.group(1)?.trim()
                } else {
                    val fullMatcher = SPEED_PATTERN.matcher(fullText)
                    if (fullMatcher.find()) {
                        speedText = fullMatcher.group(1)?.trim()
                    }
                }
            }
        }

        // Trạng thái cài đặt / chờ
        val isInstalling = lower.contains("đang cài đặt") || lower.contains("installing")
        val isPending = lower.contains("đang chờ") || lower.contains("pending") || lower.contains("chuẩn bị") || lower.contains("preparing")

        var progress = 0
        var isIndeterminate = extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false)

        val rawMax = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0)
        val rawProgress = extras.getInt(NotificationCompat.EXTRA_PROGRESS, 0)

        if (rawMax > 0 && !isIndeterminate) {
            progress = (rawProgress * 100 / rawMax).coerceIn(0, 100)
        } else {
            // Thử trích xuất % từ chuỗi văn bản
            val mPercent = PERCENT_PATTERN.matcher(fullText)
            if (mPercent.find()) {
                val p = mPercent.group(1)?.toIntOrNull()
                if (p != null) {
                    progress = p.coerceIn(0, 100)
                    isIndeterminate = false
                }
            } else {
                // Thử trích xuất theo dung lượng A / B
                val mSize = SIZE_PATTERN.matcher(fullText)
                if (mSize.find()) {
                    val current = mSize.group(1)?.replace(',', '.')?.toFloatOrNull() ?: 0f
                    val total = mSize.group(2)?.replace(',', '.')?.toFloatOrNull() ?: 0f
                    if (total > 0f) {
                        progress = ((current / total) * 100).toInt().coerceIn(0, 100)
                        isIndeterminate = false
                    }
                }
            }
        }

        if (isInstalling) {
            isIndeterminate = true
            if (progress <= 0) progress = 99
        } else if (isPending) {
            isIndeterminate = true
            progress = 0
        } else if (isFinished) {
            progress = 100
            isIndeterminate = false
        }

        // Tên file hoặc tên ứng dụng đang tải
        val displayTitle = when {
            title.isNotBlank() && !title.lowercase().contains("cửa hàng") && !title.lowercase().contains("play store") && !title.lowercase().contains("download") -> title
            text.isNotBlank() && !text.lowercase().contains("đang tải") && !text.lowercase().contains("downloading") -> text
            else -> when (pkg) {
                "com.android.vending" -> "Cửa hàng Google Play"
                "com.android.chrome" -> "Tải tệp Chrome"
                "com.microsoft.emmx" -> "Tải tệp Edge"
                else -> "Đang tải xuống"
            }
        }

        // Mô tả chi tiết trạng thái
        val displayStatus = when {
            isFinished -> "Đã tải xuống thành công"
            isInstalling -> "Đang cài đặt ứng dụng…"
            isPending -> "Đang chờ tải xuống…"
            progress > 0 && !speedText.isNullOrBlank() -> "Đang tải $progress% · $speedText"
            progress > 0 -> "Đang tải xuống $progress%"
            !speedText.isNullOrBlank() -> "Đang tải · $speedText"
            text.isNotBlank() -> text
            else -> "Đang tải xuống…"
        }

        return DownloadInfo(
            title = displayTitle,
            statusText = displayStatus,
            progress = progress,
            isIndeterminate = isIndeterminate,
            isFinished = isFinished,
            speedText = speedText,
            contentIntent = n.contentIntent,
        )
    }
}
