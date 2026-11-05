package com.originisle.android.quickshare

import android.app.Notification
import android.app.PendingIntent
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
        val pkg = sbn.packageName
        if (pkg !in QUICK_SHARE_PACKAGES && !pkg.contains("easyshare") && !pkg.contains("quickshare")) {
            return false
        }

        val n = sbn.notification
        val channelId = n.channelId?.lowercase().orEmpty()
        val extras = n.extras
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val allText = "$title $text $bigText $subText".lowercase()

        return when (pkg) {
            "com.google.android.gms" -> {
                val isChannelMatch = channelId.contains("nearby") ||
                    channelId.contains("sharing") ||
                    channelId.contains("quick_share") ||
                    channelId.contains("quickshare")
                val isTextMatch = allText.contains("quick share") ||
                    allText.contains("chia sẻ lân cận") ||
                    allText.contains("nearby share") ||
                    allText.contains("chia sẻ nhanh")
                isChannelMatch || isTextMatch
            }
            "com.google.android.apps.sharing",
            "com.vivo.easyshare",
            "com.samsung.android.app.share" -> true
            else -> {
                allText.contains("quick share") ||
                    allText.contains("easyshare") ||
                    allText.contains("nearby share") ||
                    allText.contains("chia sẻ")
            }
        }
    }

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

        val isRequest = (hasAccept || hasDecline || isRequestByText) && rawActions.isNotEmpty()

        // 2. Progress & Finished detection
        val max = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0)
        val curr = extras.getInt(NotificationCompat.EXTRA_PROGRESS, 0)
        val isIndeterminate = extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false)

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

        val isFinished = progress >= 100 ||
            allTextLower.contains("đã nhận") ||
            allTextLower.contains("hoàn tất") ||
            allTextLower.contains("đã gửi") ||
            allTextLower.contains("complete") ||
            allTextLower.contains("sent") ||
            allTextLower.contains("received") ||
            allTextLower.contains("thành công") ||
            allTextLower.contains("đã chia sẻ")

        // 3. Speed detection
        val sm = SPEED_PATTERN.matcher(allCombined)
        val speedText = if (sm.find()) sm.group(1) else null

        // 4. File name detection
        val fm = FILE_PATTERN.matcher(allCombined)
        val fileName = if (fm.find()) fm.group(1)?.trim() else null

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
            isRequest && deviceName != null -> "Chia sẻ từ $deviceName"
            isRequest -> title.ifBlank { "Yêu cầu Quick Share" }
            deviceName != null -> "$deviceName · Quick Share"
            title.isNotBlank() -> title
            else -> "Quick Share"
        }

        val displayStatus = when {
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
            speedText = speedText,
            isRequest = isRequest,
            actions = rawActions.take(3),
            contentIntent = sbn.notification.contentIntent,
        )
    }
}
