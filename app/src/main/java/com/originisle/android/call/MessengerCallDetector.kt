package com.originisle.android.call

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

data class MessengerCallInfo(
    val callerName: String,
    val isVideo: Boolean,
    val isIncoming: Boolean,
    val answerIntent: PendingIntent?,
    val declineIntent: PendingIntent?,
    val contentIntent: PendingIntent?,
    val avatar: Bitmap?
)

/**
 * Bộ nhận diện và bóc tách chuyên sâu thông báo cuộc gọi Facebook Messenger (`com.facebook.orca`).
 * Trích xuất người gọi, phân loại cuộc gọi thoại/video, phân biệt trạng thái gọi đến / đang đàm thoại,
 * cùng các PendingIntent tương tác (Trả lời / Từ chối / Kết thúc).
 */
object MessengerCallDetector {

    const val MESSENGER_PKG = "com.facebook.orca"

    private val CALL_KEYWORDS = listOf(
        "cuộc gọi",
        "cuộc gọi đến",
        "cuộc gọi đi",
        "đang gọi cho bạn",
        "đang gọi...",
        "đang đổ chuông",
        "cuộc gọi thoại",
        "cuộc gọi video",
        "nhấc máy",
        "đang trong cuộc gọi",
        "cuộc gọi đang diễn ra",
        "incoming call",
        "ongoing call",
        "video call",
        "audio call",
        "voice call",
        "calling",
        "ringing",
    )

    private val DECLINE_KEYWORDS = listOf(
        "từ chối",
        "decline",
        "reject",
        "kết thúc",
        "end",
        "hang up",
        "hủy",
        "tắt máy",
        "bỏ qua",
    )

    private val ANSWER_KEYWORDS = listOf(
        "trả lời",
        "answer",
        "chấp nhận",
        "nhấc máy",
        "nghe",
        "accept",
    )

    /**
     * Kiểm tra xem thông báo có thuộc về cuộc gọi Facebook Messenger hay không.
     */
    fun isMessengerCall(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != MESSENGER_PKG) return false
        val n = sbn.notification ?: return false
        val extras = n.extras

        if (n.category == Notification.CATEGORY_CALL) return true
        if (n.fullScreenIntent != null) return true

        val template = extras?.getString(NotificationCompat.EXTRA_TEMPLATE).orEmpty()
        if (template.contains("CallStyle", ignoreCase = true)) return true

        val channelId = n.channelId?.lowercase().orEmpty()
        if (channelId.contains("call") || channelId.contains("calling") ||
            channelId.contains("rtc") || channelId.contains("voip")
        ) {
            return true
        }

        val actions = n.actions
        if (actions != null && actions.isNotEmpty()) {
            val hasCallAction = actions.any { action ->
                val title = action.title?.toString()?.lowercase().orEmpty()
                DECLINE_KEYWORDS.any { title.contains(it) } || ANSWER_KEYWORDS.any { title.contains(it) }
            }
            if (hasCallAction) return true
        }

        val combinedTexts = buildString {
            append(extras?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty())
            append(' ')
            append(extras?.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty())
            append(' ')
            append(extras?.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty())
            append(' ')
            append(extras?.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString().orEmpty())
            append(' ')
            append(n.tickerText?.toString().orEmpty())
        }.lowercase()

        return CALL_KEYWORDS.any { combinedTexts.contains(it) }
    }

    /**
     * Bóc tách thông tin cuộc gọi Messenger từ StatusBarNotification.
     */
    fun parse(context: Context, sbn: StatusBarNotification): MessengerCallInfo? {
        if (!isMessengerCall(sbn)) return null
        val n = sbn.notification ?: return null
        val extras = n.extras ?: Bundle.EMPTY

        val extraTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val extraText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val extraBigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val extraSubText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val personName = extractCallPersonName(extras)

        val callerName = when {
            !personName.isNullOrBlank() -> personName
            extraTitle.isNotBlank() -> extraTitle
            extraBigText.isNotBlank() -> extraBigText
            extraText.isNotBlank() -> extraText
            else -> "Người dùng Messenger"
        }

        val textToCheck = "$callerName $extraTitle $extraText $extraBigText $extraSubText ${n.channelId.orEmpty()}".lowercase()
        val actionsTitleText = n.actions?.joinToString(" ") { it.title?.toString().orEmpty() }?.lowercase().orEmpty()
        val isVideo = textToCheck.contains("video") || actionsTitleText.contains("video")

        val declineFromActions = findActionIntent(n, DECLINE_KEYWORDS)
        val answerFromActions = findActionIntent(n, ANSWER_KEYWORDS)

        val hangUpExtraPi = runCatching {
            @Suppress("DEPRECATION")
            extras.getParcelable("android.hangUpIntent") as? PendingIntent
        }.getOrNull()

        val answerExtraPi = runCatching {
            @Suppress("DEPRECATION")
            extras.getParcelable("android.answerIntent") as? PendingIntent
        }.getOrNull()

        val declineIntent = hangUpExtraPi ?: declineFromActions
        val answerIntent = answerExtraPi ?: answerFromActions

        val isOngoingKeyword = textToCheck.contains("đang trong cuộc gọi") ||
            textToCheck.contains("cuộc gọi đang diễn ra") ||
            textToCheck.contains("ongoing") ||
            textToCheck.contains("đang gọi...") ||
            textToCheck.contains("00:") ||
            textToCheck.contains("in call")

        val isIncoming = when {
            answerIntent != null -> true
            isOngoingKeyword -> false
            n.fullScreenIntent != null -> true
            declineIntent != null -> false
            else -> !sbn.isOngoing
        }

        val contentIntent = n.contentIntent
            ?: n.fullScreenIntent
            ?: answerIntent
            ?: declineIntent

        val avatar = extractLargeIconBitmap(context, n, extras)

        return MessengerCallInfo(
            callerName = callerName,
            isVideo = isVideo,
            isIncoming = isIncoming,
            answerIntent = answerIntent,
            declineIntent = declineIntent,
            contentIntent = contentIntent,
            avatar = avatar,
        )
    }

    private fun extractCallPersonName(extras: Bundle): String? {
        return runCatching {
            @Suppress("DEPRECATION")
            val p = extras.getParcelable("android.callPerson") as? Person
            p?.name?.toString()?.trim()
        }.getOrNull() ?: runCatching {
            @Suppress("DEPRECATION")
            val peopleList = extras.getParcelableArrayList<Person>("android.people.list")
            peopleList?.firstOrNull()?.name?.toString()?.trim()
        }.getOrNull()
    }

    private fun extractLargeIconBitmap(context: Context, n: Notification, extras: Bundle): Bitmap? {
        return runCatching {
            @Suppress("DEPRECATION")
            when (val raw = extras.get(NotificationCompat.EXTRA_LARGE_ICON)) {
                is Bitmap -> raw
                is Icon -> iconToBitmap(context, raw)
                else -> {
                    n.getLargeIcon()?.let { iconToBitmap(context, it) }
                        ?: (extras.get(NotificationCompat.EXTRA_LARGE_ICON_BIG) as? Bitmap)
                }
            }
        }.getOrNull()
    }

    private fun iconToBitmap(context: Context, icon: Icon): Bitmap? {
        return runCatching {
            val drawable = icon.loadDrawable(context) ?: return null
            val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceIn(64, 256) else 160
            val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceIn(64, 256) else 160
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bmp
        }.getOrNull()
    }

    private fun findActionIntent(n: Notification, keywords: List<String>): PendingIntent? {
        return n.actions?.firstOrNull { action ->
            val title = action.title?.toString()?.lowercase().orEmpty()
            keywords.any { title.contains(it) }
        }?.actionIntent
    }
}
