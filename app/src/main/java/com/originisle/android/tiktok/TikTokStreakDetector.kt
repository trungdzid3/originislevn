package com.originisle.android.tiktok

import android.app.PendingIntent
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/**
 * Bộ nhận diện và trích xuất thông tin chuỗi trò chuyện TikTok (TikTok Streak).
 * Hỗ trợ mọi phiên bản TikTok phổ biến (quốc tế, Việt Nam, Douyin/Aweme, Lite).
 */
object TikTokStreakDetector {

    /** Danh sách package TikTok được hỗ trợ cứng. */
    val TIKTOK_PACKAGES: Set<String> = setOf(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.aweme",
        "com.ss.android.ugc.trill.go",
        "com.zhiliaoapp.musically.go",
    )

    /**
     * Kiểm tra xem package có phải là ứng dụng TikTok hay không (hỗ trợ mọi biến thể).
     */
    fun isTikTokPackage(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg in TIKTOK_PACKAGES ||
                pkg.contains("tiktok") ||
                pkg.contains("trill") ||
                pkg.contains("musically") ||
                pkg.contains("aweme")
    }

    private val STREAK_KEYWORDS = listOf(
        "chuỗi",
        "streak",
        "lửa",
        "🔥",
        "giữ chuỗi",
        "duy trì chuỗi",
        "khôi phục chuỗi",
        "bảo vệ chuỗi",
        "lửa chuỗi",
        "thắp lại",
        "duy trì",
        "khôi phục",
        "sắp hết hạn",
        "sắp kết thúc",
        "hết hạn",
        "mất chuỗi",
        "gửi tin nhắn",
        "đã gửi cho bạn một tin nhắn",
        "đã gửi một tin nhắn",
        "tin nhắn mới",
        "đã gửi video",
        "trò chuyện",
        "tin nhắn",
        "nhắn tin",
        "restore streak",
        "keep streak",
    )

    private val STREAK_CHANNELS = listOf(
        "streak",
        "dm_streak",
        "im_push",
        "im",
        "chat",
        "message",
        "dm",
    )

    val DAYS_REGEX = Regex(
        """(?:(\d+)\s*(?:ngày|day|days|🔥|lửa)|🔥\s*(\d+)|(\d+)\s*🔥|(?:chuỗi|streak)\s*(\d+))""",
        RegexOption.IGNORE_CASE
    )

    val WITH_FRIEND_REGEX = Regex(
        """(?:với|with|từ|from)\s+([^\n,.:!🔥?]+)""",
        RegexOption.IGNORE_CASE
    )

    private val SENT_MSG_REGEX = Regex(
        """^([^\n:!?,🔥]{1,35}?)\s+(?:đã gửi|vừa gửi|gửi|sent)\b""",
        RegexOption.IGNORE_CASE
    )

    private val CHAT_COLON_REGEX = Regex(
        """^([^\n:!?,🔥]{1,35}?)\s*:\s*.+"""
    )

    private val SYSTEM_WORDS = setOf(
        "tiktok",
        "tin nhắn",
        "tin nhắn mới",
        "chuỗi",
        "chuỗi trò chuyện",
        "streak",
        "streaks",
        "lửa",
        "lửa chuỗi",
        "thông báo",
        "bạn",
        "bạn bè",
        "hệ thống",
        "direct message",
        "messages",
        "chat",
        "tiktok live",
        "cuộc trò chuyện",
    )

    private val WITH_FRIEND_STOP_WORDS = listOf(
        " ngay", " trước", " để", " hôm nay", " trong", " nhé", " nào", " vào", " lúc"
    )

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
        if (!isTikTokPackage(pkg)) {
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
        val streakDays = extractStreakDays(fullContent)

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

    /**
     * Trích xuất số ngày chuỗi từ nội dung thông báo.
     */
    fun extractStreakDays(fullContent: String): Int {
        val match = DAYS_REGEX.find(fullContent) ?: return 0
        for (i in 1 until match.groupValues.size) {
            val str = match.groupValues[i]
            if (str.isNotBlank()) {
                val days = str.toIntOrNull()
                if (days != null && days > 0) {
                    return days
                }
            }
        }
        return 0
    }

    /**
     * Trích xuất tên bạn bè thông minh từ thông báo.
     */
    private fun extractFriendName(title: String, text: String, bigText: String): String {
        // 1. Thử tìm từ mẫu "[Tên] đã gửi..." trong title hoặc text
        for (candidate in listOf(title, text, bigText)) {
            if (candidate.isBlank()) continue
            val match = SENT_MSG_REGEX.find(candidate)
            if (match != null) {
                val name = cleanFriendNameCandidate(match.groupValues[1])
                if (isValidFriendName(name)) {
                    return name
                }
            }
        }

        // 2. Thử tìm từ mẫu "[Tên]: [Nội dung]" trong title hoặc text
        for (candidate in listOf(title, text, bigText)) {
            if (candidate.isBlank()) continue
            val match = CHAT_COLON_REGEX.find(candidate)
            if (match != null) {
                val name = cleanFriendNameCandidate(match.groupValues[1])
                if (isValidFriendName(name)) {
                    return name
                }
            }
        }

        // 3. Thử tìm từ mẫu "với [Tên]" hoặc "từ [Tên]" trong text, bigText, title
        for (candidate in listOf(text, bigText, title)) {
            if (candidate.isBlank()) continue
            val match = WITH_FRIEND_REGEX.find(candidate)
            if (match != null) {
                val name = cleanFriendNameCandidate(match.groupValues[1])
                if (isValidFriendName(name)) {
                    return name
                }
            }
        }

        // 4. Kiểm tra title trực tiếp (nếu title chính là tên bạn bè gửi đến)
        if (title.isNotBlank()) {
            val cleanTitle = cleanFriendNameCandidate(
                title.replace(Regex("""(?i)\b(?:chuỗi|streak|lửa)\b|🔥"""), "")
            )
            if (isValidFriendName(cleanTitle)) {
                return cleanTitle
            }
        }

        // 5. Fallback mặc định
        return "Bạn bè"
    }

    private fun cleanFriendNameCandidate(raw: String): String {
        var name = raw.trim()
        name = name.replace(Regex("""[\uD83C-\uDBFF\uDC00-\uDFFF\u2600-\u26FF\u2700-\u27BF]"""), "").trim()
        for (sw in WITH_FRIEND_STOP_WORDS) {
            val idx = name.indexOf(sw, ignoreCase = true)
            if (idx > 0) {
                name = name.substring(0, idx).trim()
            }
        }
        name = name.trim(',', '.', ':', '!', '?', '-', '_', ' ', '·', '•')
        return name
    }

    private fun isValidFriendName(name: String): Boolean {
        if (name.isBlank() || name.length > 35) return false
        val lower = name.lowercase().trim()
        if (lower in SYSTEM_WORDS) return false
        if (SYSTEM_WORDS.any { lower == it || lower.startsWith("$it ") || lower.startsWith("$it:") }) return false
        return true
    }
}
