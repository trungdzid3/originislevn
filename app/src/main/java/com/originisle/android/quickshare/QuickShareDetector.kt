package com.originisle.android.quickshare

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.util.regex.Pattern

data class QuickShareInfo(
    val title: String,
    val text: String,
    val deviceName: String? = null,
    val fileName: String? = null,
    val progress: Int = 0, // 0..100
    val isIndeterminate: Boolean = false,
    val isFinished: Boolean = false,
    val isCanceled: Boolean = false,
    val speedText: String? = null,
    val isRequest: Boolean = false,
    val actions: List<Notification.Action> = emptyList(),
    val contentIntent: PendingIntent? = null,
)

object QuickShareDetector {

    private val QUICK_SHARE_PACKAGES = setOf(
        "com.google.android.gms",
        "com.google.android.apps.sharing",
        "com.vivo.easyshare",
        "com.vivo.share",
        "com.bbk.share",
        "com.android.bluetooth",
        "com.samsung.android.app.share",
    )

    private val PERCENT_PATTERN = Pattern.compile("(\\d{1,3})\\s*%")
    private val SPEED_PATTERN = Pattern.compile(
        "(\\d+(?:[.,]\\d+)?\\s*(?:KB/s|MB/s|GB/s|kB/s|kb/s|mb/s|B/s|b/s|K/s|M/s))",
        Pattern.CASE_INSENSITIVE,
    )
    private val FILE_PATTERN = Pattern.compile(
        "([\\w\\-.\\s()]+\\.(?:apk|zip|rar|7z|tar|gz|bin|iso|pdf|mp4|mkv|jpg|jpeg|png|heic|mp3|docx|xlsx|txt))\\b",
        Pattern.CASE_INSENSITIVE,
    )

    fun isQuickShare(sbn: StatusBarNotification): Boolean {
        val pkg = sbn.packageName.lowercase()
        val n = sbn.notification
        val channelId = n.channelId?.lowercase().orEmpty()
        val tag = sbn.tag?.lowercase().orEmpty()
        val extras = n.extras
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val allText = "$title $text $bigText $subText".lowercase()

        val actionTitles = n.actions?.mapNotNull { it.title?.toString()?.lowercase()?.trim() }.orEmpty()

        // 1. Kiểm tra Tag
        val isTagMatch = tag.contains("sharing") || tag.contains("nearby") || tag.contains("quick")

        // 2. Kiểm tra Action buttons (Chấp nhận / Từ chối / Accept / Decline...)
        val isActionMatch = actionTitles.any { a ->
            a.contains("chấp nhận") || a.contains("accept") || a.contains("đồng ý") ||
                a == "nhận" || a.contains("nhận tệp") ||
                a.contains("từ chối") || a.contains("decline") || a.contains("reject")
        }

        // 3. Kiểm tra Channel ID
        val isChannelMatch = channelId.contains("nearby") ||
            channelId.contains("shar") ||
            channelId.contains("quick") ||
            channelId.contains("transfer") ||
            channelId.contains("fast_pair") ||
            channelId.contains("receive") ||
            channelId.contains("send") ||
            channelId.contains("file")

        // 4. Kiểm tra Text
        val isTextMatch = allText.contains("quick share") ||
            allText.contains("chia sẻ nhanh") ||
            allText.contains("chia sẻ lân cận") ||
            allText.contains("nearby share") ||
            allText.contains("đang gửi") ||
            allText.contains("đang nhận") ||
            allText.contains("sending") ||
            allText.contains("receiving") ||
            allText.contains("đã nhận") ||
            allText.contains("đã gửi") ||
            allText.contains("tệp") ||
            allText.contains("file") ||
            allText.contains("wants to share") ||
            allText.contains("muốn chia sẻ") ||
            allText.contains("đã chia sẻ") ||
            allText.contains("hủy") ||
            allText.contains("cancel") ||
            allText.contains("declined")

        // 5. Kiểm tra Progress
        val max = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0)
        val hasProgress = max > 0 ||
            extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false) ||
            PERCENT_PATTERN.matcher(allText).find()

        // Gói Google Play Services (nơi chạy Nearby Share / Quick Share mặc định trên mọi máy Android):
        if (pkg == "com.google.android.gms") {
            return isActionMatch || isChannelMatch || isTagMatch || isTextMatch || hasProgress
        }

        // Các gói Quick Share chuyên biệt:
        if (pkg in QUICK_SHARE_PACKAGES ||
            pkg.contains("easyshare") ||
            pkg.contains("quickshare")
        ) {
            return true
        }

        // Gói Bluetooth truyền nhận tệp:
        if (pkg == "com.android.bluetooth") {
            return isChannelMatch || isTextMatch || isTagMatch || isActionMatch ||
                allText.contains("bluetooth") || allText.contains("chia sẻ") ||
                allText.contains("tệp") || allText.contains("file")
        }

        // Bất kỳ gói nào có tên chứa "share":
        if (pkg.contains("share")) {
            return isChannelMatch || isTextMatch || isTagMatch || isActionMatch || hasProgress
        }

        // Các gói khác: nếu có action chấp nhận/từ chối kèm channel/tag/text liên quan
        return (isActionMatch || isTagMatch || isChannelMatch) && (isTextMatch || hasProgress)
    }

    fun detect(sbn: StatusBarNotification, context: Context? = null): QuickShareInfo? = parse(sbn)

    fun parse(sbn: StatusBarNotification): QuickShareInfo? {
        if (!isQuickShare(sbn)) return null

        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val allCombined = "$title $text $bigText $subText"
        val allTextLower = allCombined.lowercase()

        // 1. Actions usable
        val rawActions = n.actions.orEmpty().filter { a ->
            a.actionIntent != null &&
                a.remoteInputs.isNullOrEmpty() &&
                !a.isContextual &&
                a.semanticAction != Notification.Action.SEMANTIC_ACTION_REPLY &&
                !a.title.isNullOrBlank()
        }

        val actionTitles = rawActions.mapNotNull { it.title?.toString()?.lowercase()?.trim() }
        val hasAccept = actionTitles.any {
            it.contains("chấp nhận") || it.contains("accept") ||
                it.contains("đồng ý") || it == "nhận" || it.contains("nhận tệp")
        }
        val hasDecline = actionTitles.any {
            it.contains("từ chối") || it.contains("decline") ||
                it.contains("reject")
        }

        val isRequestByText = allTextLower.contains("muốn chia sẻ") ||
            allTextLower.contains("wants to share") ||
            allTextLower.contains("yêu cầu nhận") ||
            allTextLower.contains("yêu cầu chia sẻ") ||
            allTextLower.contains("incoming share") ||
            allTextLower.contains("chia sẻ tệp với bạn") ||
            allTextLower.contains("muốn gửi")

        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0 || sbn.isOngoing

        // 4. File name detection (moved up for isFinished evaluation)
        val fm = FILE_PATTERN.matcher(allCombined)
        val fileName = if (fm.find()) fm.group(1)?.trim() else null

        // 2. Progress & Finished detection
        val max = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0)
        val curr = extras.getInt(NotificationCompat.EXTRA_PROGRESS, 0)
        var isIndeterminate = extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false)

        var progress = if (max > 0) {
            (curr * 100 / max).coerceIn(0, 100)
        } else {
            0
        }

        if (progress == 0) {
            val m = PERCENT_PATTERN.matcher(allCombined)
            if (m.find()) {
                progress = m.group(1)?.toIntOrNull()?.coerceIn(0, 100) ?: 0
            }
        }

        val hasFinishKeywords = allTextLower.contains("đã nhận") ||
            allTextLower.contains("hoàn tất") ||
            allTextLower.contains("đã gửi") ||
            allTextLower.contains("complete") ||
            allTextLower.contains("completed") ||
            allTextLower.contains("sent") ||
            allTextLower.contains("received") ||
            allTextLower.contains("thành công") ||
            allTextLower.contains("success") ||
            allTextLower.contains("đã chia sẻ") ||
            allTextLower.contains("đã lưu") ||
            allTextLower.contains("lưu vào") ||
            allTextLower.contains("saved") ||
            allTextLower.contains("sẵn sàng") ||
            allTextLower.contains("ready") ||
            allTextLower.contains("chạm để mở") ||
            allTextLower.contains("chạm để xem") ||
            allTextLower.contains("tap to view") ||
            allTextLower.contains("tap to open")

        val isFinished = progress >= 100 ||
            hasFinishKeywords ||
            (!isOngoing && !isRequestByText && (max <= 0 || progress <= 0) && (fileName != null || allTextLower.contains("tệp") || allTextLower.contains("file")))

        val isCanceled = !isFinished && (
            allTextLower.contains("đã hủy") ||
            allTextLower.contains("từ chối") ||
            allTextLower.contains("declined") ||
            allTextLower.contains("canceled") ||
            allTextLower.contains("cancelled") ||
            allTextLower.contains("failed") ||
            allTextLower.contains("thất bại") ||
            allTextLower.contains("không thành công")
        ) && !hasAccept

        val isRequest = !isFinished && !isCanceled && (hasAccept || hasDecline || isRequestByText)

        // Nếu indeterminate hoặc đang kết nối chờ xác nhận, gán indeterminate = true, progress = 0
        val isConnectingOrWaiting = isRequest ||
            allTextLower.contains("đang kết nối") ||
            allTextLower.contains("connecting") ||
            allTextLower.contains("chờ xác nhận") ||
            allTextLower.contains("waiting") ||
            allTextLower.contains("chuẩn bị") ||
            allTextLower.contains("preparing")

        if (!isFinished && !isCanceled) {
            if (isOngoing && (isIndeterminate || isConnectingOrWaiting || (max > 0 && progress <= 0))) {
                isIndeterminate = true
                if (progress <= 0) progress = 0
            }
        }

        // 3. Speed detection
        val sm = SPEED_PATTERN.matcher(allCombined)
        val speedText = if (sm.find()) sm.group(1) else null

        // 5. Device name detection
        val genericNames = listOf("quick share", "chia sẻ lân cận", "nearby share", "easyshare", "chia sẻ nhanh", "easy share")
        val isGenericTitle = genericNames.any { title.lowercase().contains(it) }
        val deviceName = if (!isGenericTitle && title.isNotBlank()) {
            title
        } else {
            val fromMatcher = Pattern.compile("(?:từ|from|của)\\s+([^.,:;\\n]+)", Pattern.CASE_INSENSITIVE).matcher(allCombined)
            if (fromMatcher.find()) {
                fromMatcher.group(1)?.trim()?.take(30)
            } else {
                null
            }
        }

        val displayTitle = when {
            isCanceled -> "Đã hủy"
            isRequest && deviceName != null -> "Chia sẻ từ $deviceName"
            isRequest -> title.ifBlank { "Yêu cầu Quick Share" }
            deviceName != null -> "$deviceName · Quick Share"
            title.isNotBlank() -> title
            else -> "Quick Share"
        }

        val displayStatus = when {
            isCanceled -> "✕"
            isRequest -> text.ifBlank { "Chạm hoặc chọn Chấp nhận để nhận tệp" }
            isFinished -> "Đã hoàn tất truyền tệp"
            !speedText.isNullOrBlank() && fileName != null -> "$fileName · $speedText"
            !speedText.isNullOrBlank() -> "${text.ifBlank { "Đang truyền" }} · $speedText"
            fileName != null -> fileName
            text.isNotBlank() -> text
            else -> "Đang truyền tệp"
        }

        return QuickShareInfo(
            title = displayTitle,
            text = displayStatus,
            deviceName = deviceName,
            fileName = fileName,
            progress = progress,
            isIndeterminate = isIndeterminate,
            isFinished = isFinished,
            isCanceled = isCanceled,
            speedText = speedText,
            isRequest = isRequest,
            actions = rawActions.take(3),
            contentIntent = sbn.notification.contentIntent,
        )
    }
}
