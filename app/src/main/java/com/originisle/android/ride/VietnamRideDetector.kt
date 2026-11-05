package com.originisle.android.ride

import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.util.regex.Pattern

object VietnamRideDetector {

    const val PKG_GRAB = "com.grabtaxi.passenger"
    const val PKG_BE = "xyz.be.customer"
    const val PKG_XANH_SM = "com.gsm.customer"
    const val PKG_SHOPEE = "com.shopee.vn"

    private val SUPPORTED_PACKAGES = setOf(PKG_GRAB, PKG_BE, PKG_XANH_SM, PKG_SHOPEE)

    // Regex bien so xe Viet Nam: 29A-123.45, 59X3-12345, 43B1-999.99
    private val PLATE_REGEX = Pattern.compile("(?i)\\b(\\d{2}[A-Z0-9]{1,2}[-. ]?\\d{3,5}(?:\\.\\d{2})?)\\b")

    // Regex thoi gian: 3 phut, 5 min
    private val ETA_REGEX = Pattern.compile("(?i)(\\d{1,2})\\s*(?:phút|min|mins)")

    fun isSupported(packageName: String): Boolean {
        return SUPPORTED_PACKAGES.contains(packageName)
    }

    fun parse(sbn: StatusBarNotification): RideInfo? {
        val pkg = sbn.packageName
        if (!isSupported(pkg)) return null

        val extras = sbn.notification.extras ?: return null
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()

        val fullContent = "$title $text $bigText".trim()
        if (fullContent.isBlank()) return null

        val plateMatcher = PLATE_REGEX.matcher(fullContent)
        val licensePlate = if (plateMatcher.find()) plateMatcher.group(1)?.uppercase() else null

        val etaMatcher = ETA_REGEX.matcher(fullContent)
        val eta = if (etaMatcher.find()) etaMatcher.group(1)?.toIntOrNull() else null

        return when (pkg) {
            PKG_GRAB -> parseGrab(title, text, licensePlate, eta)
            PKG_BE -> parseBe(title, text, licensePlate, eta)
            PKG_XANH_SM -> parseXanhSM(title, text, licensePlate, eta)
            PKG_SHOPEE -> parseShopeeFood(title, text, eta)
            else -> null
        }
    }

    private fun parseGrab(title: String, text: String, plate: String?, eta: Int?): RideInfo {
        val isFood = title.contains("Giao đồ ăn", ignoreCase = true) || text.contains("món", ignoreCase = true)
        return RideInfo(
            appName = if (isFood) "Giao đồ ăn" else "Chuyến đi",
            packageName = PKG_GRAB,
            title = title.ifEmpty { "Chuyến đi Grab" },
            licensePlate = plate,
            etaMinutes = eta,
            statusText = text.ifEmpty { "Đang cập nhật lộ trình" },
            type = if (isFood) RideType.DELIVERY else RideType.TAXI
        )
    }

    private fun parseBe(title: String, text: String, plate: String?, eta: Int?): RideInfo {
        val isFood = title.contains("Giao đồ ăn", ignoreCase = true) || text.contains("đơn hàng", ignoreCase = true)
        return RideInfo(
            appName = if (isFood) "Giao đồ ăn" else "Chuyến đi",
            packageName = PKG_BE,
            title = title.ifEmpty { "Chuyến đi Be" },
            licensePlate = plate,
            etaMinutes = eta,
            statusText = text.ifEmpty { "Tài xế đang di chuyển" },
            type = if (isFood) RideType.DELIVERY else RideType.TAXI
        )
    }

    private fun parseXanhSM(title: String, text: String, plate: String?, eta: Int?): RideInfo {
        return RideInfo(
            appName = "Chuyến đi",
            packageName = PKG_XANH_SM,
            title = title.ifEmpty { "Taxi điện Xanh SM" },
            licensePlate = plate,
            etaMinutes = eta,
            statusText = text.ifEmpty { "Đang điều phối xe" },
            type = RideType.TAXI
        )
    }

    private fun parseShopeeFood(title: String, text: String, eta: Int?): RideInfo {
        return RideInfo(
            appName = "Giao đồ ăn",
            packageName = PKG_SHOPEE,
            title = title.ifEmpty { "Giao đồ ăn" },
            etaMinutes = eta,
            statusText = text.ifEmpty { "Đang giao đồ ăn" },
            type = RideType.DELIVERY
        )
    }
}
