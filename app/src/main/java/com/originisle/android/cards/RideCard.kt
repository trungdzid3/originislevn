package com.originisle.android.cards

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.ride.RideInfo
import com.originisle.android.ride.RideType
import com.originisle.android.ride.VietnamRideDetector
import java.util.concurrent.ConcurrentHashMap

object RideCard {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingDismissTasks = ConcurrentHashMap<Int, Runnable>()

    fun post(context: Context, info: RideInfo, notificationId: Int) {
        val ext = VietnamRideDetector.getExtended(notificationId)
        val isCompleted = ext?.isCompleted == true
        val category = ext?.serviceCategory ?: if (info.type == RideType.DELIVERY) {
            VietnamRideDetector.ServiceCategory.FOOD
        } else {
            VietnamRideDetector.ServiceCategory.CAR
        }

        // Chọn scene phù hợp theo chuẩn vivo OriginOS SuperX (TAXI, TAKEOUT, DELIEVERY)
        val scene = when (category) {
            VietnamRideDetector.ServiceCategory.FOOD -> "TAKEOUT"
            VietnamRideDetector.ServiceCategory.EXPRESS -> "DELIEVERY"
            else -> "TAXI"
        }

        val progress = ext?.progress ?: if (isCompleted) {
            100
        } else if (info.etaMinutes != null && info.etaMinutes <= 3) {
            90
        } else {
            45
        }

        // Viên nang bên phải (oi_right_content): Hiển thị Biển số xe hoặc Model xe hoặc trạng thái chặng ngắn gọn
        val rightCapsuleContent = when {
            !info.licensePlate.isNullOrBlank() -> info.licensePlate
            !ext?.vehicleModel.isNullOrBlank() -> ext.vehicleModel
            ext?.stage != null -> ext.stage.shortLabel
            info.statusText.isNotBlank() -> info.statusText.take(12)
            else -> info.appName
        }

        // Viên nang bên trái (oi_left_content): Hiển thị ETA (ví dụ "5 phút") hoặc Icon/loại hình
        val leftCapsuleContent = when {
            info.etaMinutes != null -> "${info.etaMinutes} phút"
            category == VietnamRideDetector.ServiceCategory.FOOD -> "Món ăn"
            category == VietnamRideDetector.ServiceCategory.BIKE -> "Xe máy"
            category == VietnamRideDetector.ServiceCategory.CAR -> "Ô tô"
            category == VietnamRideDetector.ServiceCategory.EXPRESS -> "Giao hàng"
            else -> info.appName
        }

        // Tiêu đề cho Thẻ mở rộng (Expanded Card):
        // Kết hợp: App name + Tên quán (nếu là đồ ăn) HOẶC Model xe + Biển số (ví dụ: "Xanh SM Taxi • VF 5 • 29A-123.45")
        val expandedTitle = buildString {
            append(info.appName)
            if (!ext?.restaurantOrStore.isNullOrBlank()) {
                append(" • ${ext.restaurantOrStore}")
            } else {
                val vehicleDetails = listOfNotNull(
                    ext?.vehicleModel?.takeIf { it.isNotBlank() },
                    info.licensePlate?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (vehicleDetails.isNotBlank()) {
                    append(" • $vehicleDetails")
                }
            }
        }

        // Nội dung chi tiết cho Thẻ mở rộng
        val expandedContent = buildString {
            if (!info.driverName.isNullOrBlank()) {
                append("Tài xế: ${info.driverName} • ")
            }
            if (!ext?.driverMessage.isNullOrBlank() && ext.driverMessage != info.statusText) {
                append("Tin nhắn: \"${ext.driverMessage}\" • ")
            }
            append(info.statusText)
            if (info.etaMinutes != null && !isCompleted) {
                append(" • ETA: ${info.etaMinutes}p")
            }
            if (!ext?.price.isNullOrBlank()) {
                append(" • ${ext.price}")
            }
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", notificationId)
            putExtra("oi_scene", scene)
            putExtra("title", expandedTitle)
            putExtra("text", expandedContent)
            putExtra("source_pkg", info.packageName)
            putExtra("is_ongoing", !isCompleted)

            // OriginOS 6 SuperX Progress Visual Template (Template 2)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL)
            putExtra("oi_progress", progress)
            putExtra("oi_progress_color", ext?.brandColor ?: "#FF00E676")
            putExtra("oi_bg_color", "#FF1C1C1E")
            putExtra("oi_fg_color", "#FFFFFFFF")

            // Viên nang Đảo động (Capsule / Pill)
            putExtra("oi_left_content", leftCapsuleContent)
            putExtra("oi_right_content", rightCapsuleContent)
            putExtra("status_chip_text", rightCapsuleContent)

            // Nút hành động trên Thẻ mở rộng: Hỗ trợ cả Call Action và Chat Action
            val cardActions = arrayListOf<Notification.Action>()
            ext?.callAction?.let { cardActions.add(it) }
            ext?.chatAction?.let { cardActions.add(it) }

            if (cardActions.isNotEmpty()) {
                putParcelableArrayListExtra("actions", cardActions)
            } else if (!ext?.allActions.isNullOrEmpty()) {
                putParcelableArrayListExtra("actions", ArrayList(ext.allActions))
            }

            // Click response mở app gốc
            if (ext?.contentIntent != null) {
                putExtra("click_resp", ext.contentIntent)
            }

            if (isCompleted) {
                putExtra("oi_keep_duration", 8)
            }
        }

        // Hủy task tự đóng trước đó nếu có cập nhật mới
        pendingDismissTasks.remove(notificationId)?.let { mainHandler.removeCallbacks(it) }

        // Mốc 5: Hoàn tất -> Tự kết thúc sau 8s
        if (isCompleted) {
            val dismissRunnable = Runnable {
                val cancelIntent = Intent(context, PlaygroundService::class.java).apply {
                    action = PlaygroundService.ACTION_CANCEL
                    putExtra("id", notificationId)
                    putExtra("oi_scene", scene)
                }
                context.startService(cancelIntent)
                VietnamRideDetector.clearExtended(notificationId)
                pendingDismissTasks.remove(notificationId)
            }
            pendingDismissTasks[notificationId] = dismissRunnable
            mainHandler.postDelayed(dismissRunnable, 8000L)
        }

        context.startService(intent)
    }
}
