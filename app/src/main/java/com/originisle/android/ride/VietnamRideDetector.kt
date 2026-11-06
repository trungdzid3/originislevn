package com.originisle.android.ride

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object VietnamRideDetector {

    const val PKG_GRAB = "com.grabtaxi.passenger"
    const val PKG_BE = "xyz.be.customer"
    const val PKG_XANH_SM = "com.gsm.customer"
    const val PKG_SHOPEE_FOOD = "com.deliverynow"
    const val PKG_SHOPEE = "com.shopee.vn"
    const val PKG_GOJEK = "com.gojek.app"
    const val PKG_AHAMOVE = "com.ahamove.consumer"

    private val SUPPORTED_PACKAGES = setOf(
        PKG_GRAB,
        PKG_BE,
        PKG_XANH_SM,
        PKG_SHOPEE_FOOD,
        PKG_SHOPEE,
        PKG_GOJEK,
        PKG_AHAMOVE,
    )

    enum class ServiceCategory {
        FOOD,       // Giao đồ ăn
        BIKE,       // Xe máy
        CAR,        // Ô tô / Taxi
        EXPRESS     // Giao hàng / bưu kiện
    }

    enum class RideStage(val defaultProgress: Int, val shortLabel: String) {
        FINDING(20, "Tìm tài xế"),                       // Mốc 1: 20%
        DRIVER_ARRIVING(45, "Đang đến"),                 // Mốc 2: 45%
        DRIVER_ARRIVED_OR_DELIVERING(75, "Đang giao"),   // Mốc 3: 75%
        ON_TRIP_OR_APPROACHING(90, "Sắp đến"),           // Mốc 4: 90%
        COMPLETED(100, "Hoàn tất")                       // Mốc 5: 100%
    }

    data class ExtendedRideData(
        val progress: Int,
        val serviceCategory: ServiceCategory,
        val stage: RideStage,
        val price: String? = null,
        val restaurantOrStore: String? = null,
        val callAction: Notification.Action? = null,
        val allActions: List<Notification.Action> = emptyList(),
        val contentIntent: PendingIntent? = null,
        val isCompleted: Boolean = false,
        val brandColor: String = "#FF00E676"
    )

    private val extendedDataCache = ConcurrentHashMap<Int, ExtendedRideData>()

    fun getExtended(id: Int): ExtendedRideData? = extendedDataCache[id]
    fun clearExtended(id: Int) { extendedDataCache.remove(id) }

    // Regex biển số xe Việt Nam: 29A-123.45, 59X3-123.45, 43B1-999.99, 51K-12345, 30E 888.88
    private val PLATE_REGEX = Pattern.compile("(?i)\\b(\\d{2}[A-Z][0-9A-Z]?[-.\\s]?[0-9]{3,5}(?:\\.[0-9]{2})?)\\b")

    // Regex thời gian dự kiến: "5 phút", "khoảng 10 phút", "3 min", "ETA: 4 mins"
    private val ETA_REGEX = Pattern.compile("(?i)(?:khoảng|~|dự kiến|eta)?\\s*(\\d{1,2})\\s*(?:phút|phut|min|mins|p\\b)")

    // Regex giá tiền: "120.000đ", "55,000 VND", "35k", "250.000 đ"
    private val PRICE_REGEX = Pattern.compile("(?i)(?:tổng|giá|cước|tiền|thanh toán)?[:\\s]*([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{2,4}\\s*k)\\s*(?:đ|k|vnd|đồng)?\\b")

    // Regex tên tài xế: "Tài xế Nguyễn Văn A (29A-123.45)", "Shipper Tran Van B đang đến"
    private val DRIVER_NAME_REGEX = Pattern.compile("(?i)(?:tài xế|bác tài|shipper|tx|driver)\\s*[:\\-]?\\s*([\\p{L}\\s]{2,25}?)(?=\\s*[-–(]|\\[|\\s+đang|\\s+sẽ|\\s+với|\\s*$|[.,])")

    // Regex tên quán / nhà hàng (cho đơn food)
    private val RESTAURANT_REGEX = Pattern.compile("(?i)(?:quán|nhà hàng|tiệm|cửa hàng)\\s*[:\\-]?\\s*([\\p{L}0-9\\s]{2,30}?)(?=\\s*đã|\\s*đang|\\s*vừa|\\s*$|[.,\\-])")

    fun isSupported(packageName: String): Boolean {
        return SUPPORTED_PACKAGES.contains(packageName)
    }

    fun parse(sbn: StatusBarNotification): RideInfo? {
        val pkg = sbn.packageName
        if (!isSupported(pkg)) return null

        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString().orEmpty()

        val fullContent = "$title $text $bigText $subText".trim()
        if (fullContent.isBlank()) return null

        val plateMatcher = PLATE_REGEX.matcher(fullContent)
        val licensePlate = if (plateMatcher.find()) plateMatcher.group(1)?.uppercase() else null

        val etaMatcher = ETA_REGEX.matcher(fullContent)
        val eta = if (etaMatcher.find()) etaMatcher.group(1)?.toIntOrNull() else null

        val driverMatcher = DRIVER_NAME_REGEX.matcher(fullContent)
        val driverName = if (driverMatcher.find()) driverMatcher.group(1)?.trim() else null

        val priceMatcher = PRICE_REGEX.matcher(fullContent)
        val price = if (priceMatcher.find()) priceMatcher.group(1)?.trim()?.let {
            if (!it.endsWith("đ", true) && !it.endsWith("k", true) && !it.endsWith("vnd", true)) "$it đ" else it
        } else null

        val restMatcher = RESTAURANT_REGEX.matcher(fullContent)
        val restaurantName = if (restMatcher.find()) restMatcher.group(1)?.trim() else null

        val actionsList = notification.actions?.toList().orEmpty()
        val callAction = actionsList.firstOrNull { action ->
            val aTitle = action.title?.toString().orEmpty()
            aTitle.contains("Gọi", ignoreCase = true) ||
                aTitle.contains("Call", ignoreCase = true) ||
                aTitle.contains("Liên hệ", ignoreCase = true)
        }

        val serviceCategory = detectCategory(pkg, fullContent)
        val (stage, progress, isCompleted) = detectStageAndProgress(fullContent, eta)
        val brandColor = resolveBrandColor(pkg)

        val extended = ExtendedRideData(
            progress = progress,
            serviceCategory = serviceCategory,
            stage = stage,
            price = price,
            restaurantOrStore = restaurantName,
            callAction = callAction,
            allActions = actionsList,
            contentIntent = notification.contentIntent,
            isCompleted = isCompleted,
            brandColor = brandColor
        )
        extendedDataCache[sbn.id] = extended

        val rideType = if (serviceCategory == ServiceCategory.FOOD || serviceCategory == ServiceCategory.EXPRESS) {
            RideType.DELIVERY
        } else {
            RideType.TAXI
        }

        val appDisplayName = resolveAppDisplayName(pkg, serviceCategory)

        return RideInfo(
            appName = appDisplayName,
            packageName = pkg,
            title = title.ifEmpty { appDisplayName },
            driverName = driverName,
            licensePlate = licensePlate,
            etaMinutes = eta,
            statusText = text.ifEmpty { stage.shortLabel },
            type = rideType
        )
    }

    private fun detectCategory(pkg: String, content: String): ServiceCategory {
        val lower = content.lowercase()

        // 1. Dấu hiệu rõ ràng theo từ khóa
        if (lower.contains("food") || lower.contains("món") || lower.contains("quán") ||
            lower.contains("nhà hàng") || lower.contains("đồ ăn") || lower.contains("thức ăn") ||
            lower.contains("trà sữa") || lower.contains("cơm") || lower.contains("bún") ||
            lower.contains("chuẩn bị món") || lower.contains("lấy món")
        ) {
            return ServiceCategory.FOOD
        }

        if (lower.contains("express") || lower.contains("giao hàng") || lower.contains("bưu kiện") ||
            lower.contains("kiện hàng") || lower.contains("chuyển phát") || lower.contains("lấy hàng") ||
            lower.contains("gói hàng")
        ) {
            return ServiceCategory.EXPRESS
        }

        if (lower.contains("bike") || lower.contains("xe máy") || lower.contains("2 bánh") ||
            lower.contains("grabbike") || lower.contains("bebike") || lower.contains("goride")
        ) {
            return ServiceCategory.BIKE
        }

        if (lower.contains("car") || lower.contains("ô tô") || lower.contains("taxi") ||
            lower.contains("grabcar") || lower.contains("becar") || lower.contains("gocar") ||
            lower.contains("4 chỗ") || lower.contains("7 chỗ") || lower.contains("luxury")
        ) {
            return ServiceCategory.CAR
        }

        // 2. Mặc định theo Package nếu không rõ từ khóa
        return when (pkg) {
            PKG_SHOPEE_FOOD -> ServiceCategory.FOOD
            PKG_SHOPEE -> ServiceCategory.FOOD
            PKG_AHAMOVE -> ServiceCategory.EXPRESS
            PKG_XANH_SM -> ServiceCategory.CAR
            else -> ServiceCategory.BIKE
        }
    }

    private fun detectStageAndProgress(content: String, eta: Int?): Triple<RideStage, Int, Boolean> {
        val lower = content.lowercase()

        // Mốc 5: Hoàn tất / Đã đến nơi / Đã giao hàng -> 100% (tự kết thúc sau 8s)
        if (lower.contains("hoàn thành") || lower.contains("đã đến nơi") || lower.contains("kết thúc chuyến") ||
            lower.contains("cảm ơn bạn") || lower.contains("đã giao hàng") || lower.contains("giao thành công") ||
            lower.contains("đã nhận được hàng") || lower.contains("đã trả khách") || lower.contains("hoàn tất") ||
            lower.contains("chuyến đi đã xong")
        ) {
            return Triple(RideStage.COMPLETED, 100, true)
        }

        // Mốc 4: Đang trong chuyến đi / Shipper sắp đến nơi -> 90%
        if (lower.contains("đang trong chuyến") || lower.contains("bắt đầu chuyến đi") || lower.contains("đang di chuyển") ||
            lower.contains("sắp đến nơi") || lower.contains("shipper sắp đến") || lower.contains("tài xế sắp đến") ||
            lower.contains("sắp tới điểm") || lower.contains("sắp tới nơi") || lower.contains("đang chở bạn") ||
            (eta != null && eta <= 2)
        ) {
            return Triple(RideStage.ON_TRIP_OR_APPROACHING, 90, false)
        }

        // Mốc 3: Tài xế đã đến điểm đón / Tài xế đã lấy món & đang giao -> 75%
        if (lower.contains("đã đến điểm đón") || lower.contains("tài xế đã đến") || lower.contains("đã tới điểm đón") ||
            lower.contains("đã có mặt") || lower.contains("đang chờ bạn") || lower.contains("đã lấy món") ||
            lower.contains("đã lấy hàng") || lower.contains("đang giao hàng") || lower.contains("đang giao món") ||
            lower.contains("bắt đầu giao") || lower.contains("đang trên đường giao") || lower.contains("đã rời quán")
        ) {
            return Triple(RideStage.DRIVER_ARRIVED_OR_DELIVERING, 75, false)
        }

        // Mốc 2: Tài xế đang đến / Quán đang chuẩn bị món -> 45%
        if (lower.contains("tài xế đang đến") || lower.contains("đang tới đón") || lower.contains("đang đến đón") ||
            lower.contains("tài xế đang di chuyển") || lower.contains("quán đang chuẩn bị") || lower.contains("đang chuẩn bị món") ||
            lower.contains("đang làm món") || lower.contains("nhà hàng đang chuẩn bị") || lower.contains("chuẩn bị đơn hàng") ||
            lower.contains("tài xế đang tới quán") || lower.contains("đang chuẩn bị đơn") || (eta != null && eta in 3..8)
        ) {
            return Triple(RideStage.DRIVER_ARRIVING, 45, false)
        }

        // Mốc 1: Đang tìm tài xế / Quán nhận đơn / Chờ xác nhận -> 20%
        if (lower.contains("đang tìm tài xế") || lower.contains("tìm tài xế") || lower.contains("đang tìm xe") ||
            lower.contains("đang kết nối") || lower.contains("quán nhận đơn") || lower.contains("quán đã nhận") ||
            lower.contains("chờ xác nhận") || lower.contains("chờ nhà hàng") || lower.contains("chờ quán") ||
            lower.contains("đã tiếp nhận") || lower.contains("đang xử lý") || lower.contains("đã được đặt")
        ) {
            return Triple(RideStage.FINDING, 20, false)
        }

        // Fallback suy luận theo ETA nếu có
        return if (eta != null) {
            when {
                eta <= 2 -> Triple(RideStage.ON_TRIP_OR_APPROACHING, 90, false)
                eta <= 6 -> Triple(RideStage.DRIVER_ARRIVED_OR_DELIVERING, 75, false)
                eta <= 12 -> Triple(RideStage.DRIVER_ARRIVING, 45, false)
                else -> Triple(RideStage.FINDING, 25, false)
            }
        } else {
            Triple(RideStage.FINDING, 20, false)
        }
    }

    private fun resolveAppDisplayName(pkg: String, category: ServiceCategory): String {
        return when (pkg) {
            PKG_GRAB -> when (category) {
                ServiceCategory.FOOD -> "GrabFood"
                ServiceCategory.EXPRESS -> "GrabExpress"
                ServiceCategory.CAR -> "GrabCar"
                ServiceCategory.BIKE -> "GrabBike"
            }
            PKG_BE -> when (category) {
                ServiceCategory.FOOD -> "beFood"
                ServiceCategory.EXPRESS -> "beDelivery"
                ServiceCategory.CAR -> "beCar"
                ServiceCategory.BIKE -> "beBike"
            }
            PKG_XANH_SM -> when (category) {
                ServiceCategory.BIKE -> "Xanh SM Bike"
                ServiceCategory.EXPRESS -> "Xanh SM Express"
                else -> "Xanh SM Taxi"
            }
            PKG_SHOPEE_FOOD, PKG_SHOPEE -> "ShopeeFood"
            PKG_GOJEK -> when (category) {
                ServiceCategory.FOOD -> "GoFood"
                ServiceCategory.EXPRESS -> "GoSend"
                ServiceCategory.CAR -> "GoCar"
                ServiceCategory.BIKE -> "GoRide"
            }
            PKG_AHAMOVE -> "Ahamove"
            else -> "Chuyến đi"
        }
    }

    private fun resolveBrandColor(pkg: String): String {
        return when (pkg) {
            PKG_GRAB -> "#FF00B14F"       // Grab Green
            PKG_BE -> "#FFFFC107"         // Be Yellow
            PKG_XANH_SM -> "#FF00B4D8"    // Xanh SM Cyan
            PKG_SHOPEE_FOOD, PKG_SHOPEE -> "#FFEE4D2D" // Shopee Orange
            PKG_GOJEK -> "#FF00AA13"      // Gojek Green
            PKG_AHAMOVE -> "#FFFF6600"    // Aha Orange
            else -> "#FF00E676"           // Vivid Green default
        }
    }
}
