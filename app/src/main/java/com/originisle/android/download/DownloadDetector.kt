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
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.touch",
        "com.coccoc.trinhduyet",
        "com.sec.android.app.sbrowser",
        "com.sec.android.app.sbrowser.beta",
        "com.duckduckgo.mobile.android",
        "mark.via.gp",
    )

    private val MEDIA_PACKAGES = setOf(
        "com.google.android.apps.youtube.music",
        "com.google.android.youtube",
        "com.spotify.music",
        "com.zing.mp3",
        "com.apple.android.music",
        "deezer.android.app",
        "com.soundcloud.android",
        "com.netease.cloudmusic",
        "com.tencent.qqmusic",
        "com.kugou.android",
        "vn.nhaccuatui",
        "tunein.player",
    )

    private val NAV_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.samsung.wearable.fit3plugin",
        "com.samsung.android.wearable.fit3plugin",
        "com.samsung.android.waterplugin",
        "com.samsung.android.app.watchmanager",
        "com.samsung.accessory",
        "com.samsung.accessory.neobeanmgr",
        "com.samsung.accessory.atticmgr",
        "com.google.android.apps.maps",
        "com.waze",
        "app.organicmaps",
        "com.sygic.aura",
        "com.here.app.maps",
        // Ứng dụng gọi xe / giao hàng
        "com.grabtaxi.passenger",
        "com.grabtaxi.driver2",
        "be.com.vinagroup.partner",
        "com.be.customer",
        "com.vng.zalopay",
        "vn.green.gsm",
        "com.shopee.food.driver",
        "com.shopee.vn",
        "com.gojek.app",
        "com.deliveree",
        "com.ahamove",
        "com.lalamove",
    )

    private val PERCENT_PATTERN = Pattern.compile("(\\d{1,3})\\s*%")
    private val SIZE_PATTERN = Pattern.compile(
        "(\\d+(?:[.,]\\d+)?)\\s*(MB|GB|KB|kB|M|G|K|B)?\\s*(?:/|of|của|trong(?:\\s+số)?|trên)\\s*(\\d+(?:[.,]\\d+)?)\\s*(MB|GB|KB|kB|M|G|K|B)",
        Pattern.CASE_INSENSITIVE
    )
    private val SINGLE_SIZE_PATTERN = Pattern.compile(
        "(\\d+(?:[.,]\\d+)?)\\s*(?:MB|GB|KB|kB)",
        Pattern.CASE_INSENSITIVE
    )
    private val SPEED_PATTERN = Pattern.compile(
        "(\\d+(?:[.,]\\d+)?\\s*(?:KB/s|MB/s|GB/s|kB/s|kb/s|mb/s|B/s|b/s|K/s|M/s))",
        Pattern.CASE_INSENSITIVE
    )
    private val FILE_EXT_PATTERN = Pattern.compile(
        "\\.(?:apk|zip|rar|7z|tar|gz|bin|iso|pdf|mp4|mkv|exe|dmg|docx|xlsx)\\b",
        Pattern.CASE_INSENSITIVE
    )

    fun isDownloadPackage(pkg: String): Boolean {
        return pkg in DOWNLOAD_PACKAGES ||
            pkg.startsWith("com.microsoft.emmx") ||
            pkg.startsWith("com.android.chrome") ||
            pkg.startsWith("com.chrome") ||
            pkg == "com.android.vending" ||
            pkg.startsWith("com.android.providers.downloads") ||
            pkg.contains("browser")
    }

    fun isDownload(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val extras = n.extras
        val pkg = sbn.packageName

        val channelId = n.channelId?.lowercase().orEmpty()
        val isExplicitDownloadChannel = channelId.contains("download") ||
            channelId.contains("tai_xuong") ||
            channelId.contains("tải") ||
            channelId.contains("tệp") ||
            channelId.contains("transfer")

        // Kiểm tra nội dung text
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
            allText.contains("installed") ||
            allText.contains("hoàn tất tải") ||
            allText.contains("tải hoàn tất") ||
            allText.contains("download complete") ||
            allText.contains("download finished") ||
            allText.contains("đã tải về") ||
            allText.contains("tải thành công") ||
            allText.contains("tải thất bại") ||
            allText.contains("hủy tải") ||
            allText.contains("download failed") ||
            allText.contains("download canceled") ||
            allText.contains("download cancelled")

        val isDownloadPkg = isDownloadPackage(pkg)

        // 0. Bỏ qua ngay các danh mục điều hướng, di chuyển, gọi thoại, báo thức, đồng hồ bấm giờ hoặc dịch vụ ngầm
        if (n.category == Notification.CATEGORY_NAVIGATION ||
            n.category == Notification.CATEGORY_TRANSPORT ||
            n.category == Notification.CATEGORY_CALL ||
            n.category == Notification.CATEGORY_ALARM ||
            n.category == Notification.CATEGORY_STOPWATCH ||
            (n.category == Notification.CATEGORY_SERVICE && !isDownloadPkg && !isExplicitDownloadChannel && !hasDownloadKeywords)
        ) {
            return false
        }

        if (pkg in NAV_PACKAGES) {
            return false
        }

        // Bỏ qua nếu là thông báo phát đa phương tiện (Media Session / MediaStyle)
        val isMediaSession = extras.containsKey(NotificationCompat.EXTRA_MEDIA_SESSION) ||
            extras.containsKey("android.mediaSession") ||
            extras.getString(NotificationCompat.EXTRA_TEMPLATE)?.contains("MediaStyle") == true

        if (isMediaSession) {
            // Nếu thông báo mang MediaSession token thì 100% là điều khiển phát nhạc, không bao giờ là file download
            return false
        }

        if (pkg in MEDIA_PACKAGES && !isExplicitDownloadChannel) {
            // Nếu thuộc app nghe nhạc mà channel không phải download ngoại tuyến thì bỏ qua
            return false
        }

        // 1. Kiểm tra progress bar trực tiếp từ hệ thống
        val max = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false)
        if (max > 0 || indeterminate) {
            return true
        }

        // 2. Kiểm tra category chuẩn
        if (n.category == Notification.CATEGORY_PROGRESS) {
            return true
        }

        // 3. Kiểm tra kênh thông báo (Channel ID): Chrome/Edge/PlayStore luôn dùng channel có chữ download
        if (isExplicitDownloadChannel) {
            return true
        }

        // 4. Kiểm tra nội dung text
        if (hasDownloadKeywords) {
            return true
        }

        val hasDownloadMetrics = PERCENT_PATTERN.matcher(allText).find() ||
            SIZE_PATTERN.matcher(allText).find() ||
            SPEED_PATTERN.matcher(allText).find() ||
            FILE_EXT_PATTERN.matcher(allText).find() ||
            SINGLE_SIZE_PATTERN.matcher(allText).find()

        // 5. Nếu từ gói tải xuống/trình duyệt
        if (isDownloadPkg) {
            val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0

            // Play Store hoặc Android Download Manager mà có FLAG_ONGOING_EVENT thì chắc chắn là tải xuống/cài đặt
            if (isOngoing && (pkg == "com.android.vending" || pkg.startsWith("com.android.providers.downloads"))) {
                return true
            }

            // Với Chrome / Edge / Trình duyệt khác: nếu có định dạng dung lượng/tốc độ/file
            if (hasDownloadMetrics) {
                return true
            }
        }

        return false
    }

    private fun parseSizeToBytes(value: Float, unit: String): Double {
        return when (unit.uppercase()) {
            "GB", "G" -> value.toDouble() * 1024.0 * 1024.0 * 1024.0
            "MB", "M" -> value.toDouble() * 1024.0 * 1024.0
            "KB", "K" -> value.toDouble() * 1024.0
            "B" -> value.toDouble()
            else -> value.toDouble()
        }
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

        // 1. Kiểm tra trạng thái bị hủy / thất bại từ kênh (Channel ID)
        val channelLower = n.channelId?.lowercase().orEmpty()
        val channelIsCancelOrError = channelLower.contains("failed") ||
            channelLower.contains("error") ||
            channelLower.contains("cancel")

        // 2. Kiểm tra text / bigText / subText
        val statusCandidates = listOf(text, bigText, subText)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .lowercase()

        val statusIsCancelOrError = statusCandidates.contains("đã hủy") ||
            statusCandidates.contains("bị hủy") ||
            statusCandidates.contains("hủy tải") ||
            statusCandidates.contains("thất bại") ||
            statusCandidates.contains("không thành công") ||
            statusCandidates.contains("lỗi") ||
            statusCandidates.contains("canceled") ||
            statusCandidates.contains("cancelled") ||
            statusCandidates.contains("failed") ||
            statusCandidates.contains("error") ||
            statusCandidates == "hủy" ||
            statusCandidates == "cancel"

        // 3. Kiểm tra title (trừ trường hợp tên file chứa các từ khóa này, vd: error_report.pdf)
        val titleLower = title.lowercase()
        val titleIsCancelOrError = !FILE_EXT_PATTERN.matcher(title).find() && (
            titleLower.contains("thất bại") ||
            titleLower.contains("đã hủy") ||
            titleLower.contains("bị hủy") ||
            titleLower.contains("không thành công") ||
            titleLower.contains("lỗi tải") ||
            titleLower.contains("download failed") ||
            titleLower.contains("download canceled") ||
            titleLower.contains("download cancelled") ||
            titleLower == "đã hủy" ||
            titleLower == "bị hủy" ||
            titleLower == "thất bại" ||
            titleLower == "canceled" ||
            titleLower == "cancelled" ||
            titleLower == "failed"
        )

        val isCanceled = channelIsCancelOrError || statusIsCancelOrError || titleIsCancelOrError

        // Trạng thái hoàn thành (chỉ khi không bị hủy/thất bại)
        val isExplicitFinish = lower.contains("đã cài đặt") ||
            lower.contains("installed") ||
            lower.contains("tải xong") ||
            lower.contains("tải thành công") ||
            lower.contains("download complete") ||
            lower.contains("download finished") ||
            lower.contains("đã tải xong") ||
            lower.contains("đã tải về") ||
            lower.contains("thành công")

        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isFinished = !isCanceled && (isExplicitFinish || (!isOngoing && (lower.contains("hoàn tất") || lower.contains("complete"))))

        // Trích xuất tốc độ tải (speedText) từ subText/text/bigText nếu chưa hoàn tất và chưa bị hủy
        var speedText: String? = null
        if (!isFinished && !isCanceled) {
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
        val isInstalling = lower.contains("đang cài đặt") || lower.contains("installing") || lower.contains("cài đặt…")
        val isPending = lower.contains("đang chờ") || lower.contains("pending") || lower.contains("chuẩn bị") || lower.contains("preparing") || lower.contains("waiting")

        var progress = 0
        var isIndeterminate = extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false)

        val rawMax = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0)
        val rawProgress = extras.getInt(NotificationCompat.EXTRA_PROGRESS, 0)

        // Tính tiến độ từ hệ thống, dùng toLong() để chống tràn số nguyên 32-bit khi tải file lớn tính theo byte
        if (rawMax > 0) {
            progress = (rawProgress.toLong() * 100 / rawMax).toInt().coerceIn(0, 100)
            if (progress > 0) {
                isIndeterminate = false
            }
        }

        // Nếu hệ thống không cung cấp progress hoặc progress bằng 0, bóc tách từ text
        if (progress <= 0 || isIndeterminate) {
            // Thử trích xuất % từ chuỗi văn bản
            val mPercent = PERCENT_PATTERN.matcher(fullText)
            if (mPercent.find()) {
                val p = mPercent.group(1)?.toIntOrNull()
                if (p != null) {
                    progress = p.coerceIn(0, 100)
                    isIndeterminate = false
                }
            } else {
                // Thử trích xuất theo dung lượng A / B (hỗ trợ chuyển đổi đơn vị KB, MB, GB linh hoạt)
                val mSize = SIZE_PATTERN.matcher(fullText)
                if (mSize.find()) {
                    val currentVal = mSize.group(1)?.replace(',', '.')?.toFloatOrNull() ?: 0f
                    val unit1 = mSize.group(2).orEmpty()
                    val totalVal = mSize.group(3)?.replace(',', '.')?.toFloatOrNull() ?: 0f
                    val unit2 = mSize.group(4).orEmpty()
                    val currentBytes = parseSizeToBytes(currentVal, unit1.ifBlank { unit2 })
                    val totalBytes = parseSizeToBytes(totalVal, unit2)
                    if (totalBytes > 0.0) {
                        progress = ((currentBytes / totalBytes) * 100).toInt().coerceIn(0, 100)
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
        } else if (progress <= 0) {
            isIndeterminate = true
        }

        // Tên file hoặc tên ứng dụng đang tải
        val isGenericTitle = title.isBlank() ||
            title.lowercase().let {
                it.contains("cửa hàng") ||
                it.contains("play store") ||
                it.contains("download") ||
                it.contains("tải xuống") ||
                it.contains("trình duyệt") ||
                it.contains("browser") ||
                it == "chrome" ||
                it == "edge" ||
                it.contains("edge canary") ||
                it.contains("chrome canary")
            }

        val displayTitle = when {
            !isGenericTitle -> title
            text.isNotBlank() && !text.lowercase().let {
                it.contains("đang tải") || it.contains("downloading") || it.contains("tải xuống")
            } -> text
            else -> when {
                pkg == "com.android.vending" -> "Cửa hàng Google Play"
                pkg.contains("chrome") -> "Tải tệp Chrome"
                pkg.contains("emmx") -> "Tải tệp Edge"
                else -> "Đang tải xuống"
            }
        }

        // Mô tả chi tiết trạng thái
        val displayStatus = when {
            isCanceled -> when {
                statusCandidates.contains("thất bại") || statusCandidates.contains("failed") || statusCandidates.contains("lỗi") || titleLower.contains("thất bại") -> "Tải xuống thất bại"
                else -> "Đã hủy tải xuống"
            }
            isFinished -> "Đã tải xuống thành công"
            isInstalling -> "Đang cài đặt ứng dụng…"
            isPending -> "Đang chờ tải xuống…"
            progress > 0 && !speedText.isNullOrBlank() -> "Đang tải $progress% · $speedText"
            progress > 0 -> "Đang tải xuống $progress%"
            !speedText.isNullOrBlank() -> "Đang tải · $speedText"
            text.isNotBlank() -> text
            else -> "Đang tải xuống…"
        }

        // Bóc tách usable action buttons từ thông báo
        val rawActions = n.actions
        val usableActions = rawActions?.filterNotNull()?.filter { action ->
            action.actionIntent != null &&
                (action.remoteInputs == null || action.remoteInputs.isEmpty()) &&
                !action.title.isNullOrBlank()
        } ?: emptyList()

        return DownloadInfo(
            title = displayTitle,
            statusText = displayStatus,
            progress = progress,
            isIndeterminate = isIndeterminate,
            isFinished = isFinished,
            isCanceled = isCanceled,
            speedText = speedText,
            contentIntent = n.contentIntent,
            actions = usableActions,
        )
    }
}
