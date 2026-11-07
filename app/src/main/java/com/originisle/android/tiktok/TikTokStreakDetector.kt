package com.originisle.android.tiktok

import android.app.PendingIntent
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/**
 * Bộ nhận diện và trích xuất thông tin chuỗi trò chuyện TikTok (TikTok Streak).
 * Hỗ trợ các phiên bản TikTok phổ biến (quốc tế, Việt Nam, Trung Quốc/Douyin).
 */
object TikTokStreakDetector {

    /** Danh sách package TikTok được hỗ trợ. */
    val TIKTOK_PACKAGES: Set<String> = setOf(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.aweme",
    )

    private val STREAK_KEYWORDS = listOf(
        "chuỗi",
        "streak",
        "lửa",
        "🔥",
        "duy trì chuỗi",
        "khôi phục chuỗi",
        "sắp hết hạn",
    )

    private val STREAK_CHANNELS = listOf(
        "streak",
        "dm_streak",
        "im_push",
    )

    private val DAYS_REGEX = Regex("""(\d+)\s*(?:ngày|day|days|🔥)""", RegexOption.IGNORE_CASE)
    private val WITH_FRIEND_REGEX = Regex("""(?:với|with)\s+([^\n,.:!🔥?]+)""", RegexOption.IGNORE_CASE)

    /**
     * Dữ liệu bóc tách được từ thông báo chuỗi TikTok.
     */
    data class TikTokStreakInfo(
        val friendName: String,
        val message: String,
        val streakDays: Int = 0,
        val contentIntent: PendingIntent? = null,
        val sourcePkg: String,
    )

    /**
     * Kiểm tra xem thông báo có phải là thông báo chuỗi TikTok hay không.
     */
    fun isTikTokStreak(sbn: StatusBarNotification): Boolean {
        val pkg = sbn.packageName.lowercase()
        if (pkg !in TIKTOK_PACKAGES) {
            return false
        }

        val n = sbn.notification ?: return false
        val channelId = n.channelId?.lowercase().orEmpty()
        val isStreakChannel = STREAK_CHANNELS.any { channelId.contains(it) }

        val extras = n.extras ?: return isStreakChannel

        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.lowercase().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.lowercase().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.lowercase().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.lowercase().orEmpty()

        val fullText = "$title $text $bigText $subText"

        val hasStreakKeyword = STREAK_KEYWORDS.any { fullText.contains(it) }

        return isStreakChannel || hasStreakKeyword
    }

    /**
     * Phân tích và bóc tách thông tin chuỗi trò chuyện từ thông báo TikTok.
     */
    fun parse(sbn: StatusBarNotification): TikTokStreakInfo? {
        if (!isTikTokStreak(sbn)) {
            return null
        }

        val n = sbn.notification ?: return null
        val extras = n.extras

        val title = extras?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras?.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras?.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()

        val fullContent = "$title $text $bigText"

        // Bóc tách số ngày chuỗi (nếu có)
        val streakDays = DAYS_REGEX.find(fullContent)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        // Bóc tách tên bạn bè
        val friendName = extractFriendName(title, text, bigText)

        // Bóc tách tin nhắn hiển thị
        val message = when {
            text.isNotBlank() -> text
            bigText.isNotBlank() -> bigText
            else -> "Đã đến lúc gửi tin nhắn để duy trì chuỗi!"
        }

        return TikTokStreakInfo(
            friendName = friendName,
            message = message,
            streakDays = streakDays,
            contentIntent = n.contentIntent,
            sourcePkg = sbn.packageName,
        )
    }

    private fun extractFriendName(title: String, text: String, bigText: String): String {
        // 1. Thử tìm từ mẫu "với [Tên]" trong nội dung
        val matchWith = WITH_FRIEND_REGEX.find(text) ?: WITH_FRIEND_REGEX.find(bigText) ?: WITH_FRIEND_REGEX.find(title)
        if (matchWith != null) {
            val name = matchWith.groupValues[1].trim()
            if (name.isNotEmpty() && name.length <= 30) {
                return name
            }
        }

        // 2. Nếu title không phải từ khóa hệ thống, title thường chính là tên người gửi / bạn bè
        val cleanTitle = title.replace(Regex("""(?i)chuỗi|streak|🔥"""), "").trim()
        if (cleanTitle.isNotEmpty() && !cleanTitle.equals("TikTok", ignoreCase = true)) {
            return cleanTitle
        }

        return "Bạn bè"
    }
}
