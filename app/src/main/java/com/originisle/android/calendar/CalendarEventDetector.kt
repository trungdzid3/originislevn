package com.originisle.android.calendar

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/**
 * Bộ nhận diện và trích xuất thông tin Nhắc Lịch & Sự kiện sắp diễn ra (Calendar & Upcoming Event).
 * Hỗ trợ Google Calendar, Lịch Vivo OriginOS, Samsung Calendar, Microsoft Outlook và các thông báo
 * thuộc danh mục CATEGORY_EVENT / CATEGORY_REMINDER.
 */
object CalendarEventDetector {

    data class CalendarEventInfo(
        val title: String,
        val timeText: String,
        val detailText: String,
        val chipText: String,
        val sourcePkg: String,
        val contentIntent: PendingIntent?,
    )

    val CALENDAR_PACKAGES: Set<String> = setOf(
        "com.google.android.calendar",
        "com.bbk.calendar",
        "com.vivo.calendar",
        "com.samsung.android.calendar",
        "com.microsoft.office.outlook",
        "org.withouthat.acalendar",
        "com.simplemobiletools.calendar.pro",
        "com.digibites.calendar",
        "DigiCal",
    )

    val EXCLUDED_PACKAGES: Set<String> = setOf(
        "com.zing.zalo",
        "com.facebook.orca",
        "com.facebook.katana",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.ss.android.ugc.aweme",
        "com.google.android.gms",
        "android",
        "com.android.systemui",
    )

    private val OUTLOOK_EVENT_KEYWORDS = listOf(
        "lịch hẹn",
        "cuộc họp",
        "nhắc nhở",
        "sự kiện",
        "reminder",
        "meeting",
        "event",
        "starting in",
        "bắt đầu sau",
    )

    private val SYNC_NOISE_KEYWORDS = listOf(
        "đang đồng bộ",
        "syncing",
        "calendar sync",
        "đồng bộ lịch",
        "sync calendar",
    )

    val TIME_RANGE_REGEX = Regex(
        """\b([01]?\d|2[0-3]):[0-5]\d(\s*[-–—]\s*([01]?\d|2[0-3]):[0-5]\d)?\b"""
    )

    private val OUTLOOK_TIME_RANGE_REGEX = Regex(
        """\b([01]?\d|2[0-3]):[0-5]\d\s*[-–—]\s*([01]?\d|2[0-3]):[0-5]\d\b"""
    )

    val COUNTDOWN_REGEX = Regex(
        """\b(\d+)\s*(phút|giờ|min|mins|minute|minutes|hour|hours)\b""",
        RegexOption.IGNORE_CASE
    )

    val TRANSIT_CODE_REGEX = Regex(
        """\b(?:VN|VJ|QH|VU|BL|SE|TN|SPT|SNT)\s?\d{1,4}\b"""
    )

    fun isCalendarNotification(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification ?: return false
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return false
        }

        val pkg = sbn.packageName
        if (pkg in EXCLUDED_PACKAGES) {
            return false
        }

        val category = n.category
        val isEventOrReminderCategory =
            category == Notification.CATEGORY_EVENT || category == Notification.CATEGORY_REMINDER

        if (pkg == "com.microsoft.office.outlook") {
            if (isEventOrReminderCategory) return true
            val extras = n.extras ?: return false
            val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.lowercase().orEmpty()
            val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.lowercase().orEmpty()
            val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.lowercase().orEmpty()
            val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.lowercase().orEmpty()
            val combined = "$title $text $bigText $subText"

            val hasKeyword = OUTLOOK_EVENT_KEYWORDS.any { combined.contains(it) }
            val hasTimeRange = OUTLOOK_TIME_RANGE_REGEX.containsMatchIn(combined)
            return hasKeyword || hasTimeRange
        }

        if (pkg in CALENDAR_PACKAGES) {
            return true
        }

        if (isEventOrReminderCategory) {
            return true
        }

        return false
    }

    fun parse(sbn: StatusBarNotification): CalendarEventInfo? {
        if (!isCalendarNotification(sbn)) {
            return null
        }

        val n = sbn.notification ?: return null
        val extras = n.extras ?: return null

        val rawTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val rawBigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val rawSubText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()

        if (rawTitle.isBlank() && rawText.isBlank()) {
            return null
        }

        val fullLower = "$rawTitle $rawText $rawBigText $rawSubText".lowercase()
        if (SYNC_NOISE_KEYWORDS.any { fullLower.contains(it) }) {
            return null
        }

        val searchPool = listOf(rawText, rawSubText, rawBigText, rawTitle)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        val timeMatch = TIME_RANGE_REGEX.find(searchPool)
        val matchedTimeRange = timeMatch?.value?.trim().orEmpty()
        val startHourMin = matchedTimeRange.substringBefore("-").substringBefore("–").substringBefore("—").trim()

        val countdownMatch = COUNTDOWN_REGEX.find(searchPool)
        val countdownValue = countdownMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
        val countdownUnitRaw = countdownMatch?.groupValues?.getOrNull(2)?.lowercase().orEmpty()
        val isHourUnit = countdownUnitRaw.startsWith("giờ") || countdownUnitRaw.startsWith("hour")

        val transitCode = TRANSIT_CODE_REGEX.find(searchPool)?.value?.replace(" ", "").orEmpty()

        val chipText = when {
            countdownValue != null && countdownValue > 0 -> {
                val unitShort = if (isHourUnit) "h" else "p"
                "Sau ${countdownValue}${unitShort}".take(10)
            }
            startHourMin.isNotBlank() -> startHourMin.take(10)
            else -> "Sắp tới"
        }

        val timeText = when {
            matchedTimeRange.isNotBlank() -> matchedTimeRange
            countdownValue != null && countdownValue > 0 -> {
                val unitVn = if (isHourUnit) "giờ" else "phút"
                "Còn $countdownValue $unitVn"
            }
            else -> "Sắp diễn ra"
        }

        val eventTitle = when {
            rawTitle.isNotBlank() -> rawTitle
            rawText.isNotBlank() -> rawText
            else -> "Sự kiện lịch hẹn"
        }

        val primaryBody = when {
            rawBigText.isNotBlank() && rawBigText != eventTitle -> rawBigText.lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            rawText.isNotBlank() && rawText != eventTitle -> rawText
            rawSubText.isNotBlank() && rawSubText != eventTitle -> rawSubText
            else -> ""
        }

        val detailParts = mutableListOf<String>()
        if (transitCode.isNotBlank() && !eventTitle.contains(transitCode)) {
            detailParts.add("Chuyến $transitCode")
        }
        if (primaryBody.isNotBlank()) {
            detailParts.add(primaryBody)
        } else if (countdownValue != null && countdownValue > 0 && matchedTimeRange.isNotBlank()) {
            val unitVn = if (isHourUnit) "giờ" else "phút"
            detailParts.add("Còn $countdownValue $unitVn nữa bắt đầu")
        }
        if (rawSubText.isNotBlank() && rawSubText != primaryBody && rawSubText != eventTitle) {
            detailParts.add(rawSubText)
        }

        val detailText = detailParts.joinToString(" · ").ifBlank {
            "Chạm để mở chi tiết sự kiện"
        }

        return CalendarEventInfo(
            title = eventTitle,
            timeText = timeText,
            detailText = detailText,
            chipText = chipText,
            sourcePkg = sbn.packageName,
            contentIntent = n.contentIntent,
        )
    }
}
