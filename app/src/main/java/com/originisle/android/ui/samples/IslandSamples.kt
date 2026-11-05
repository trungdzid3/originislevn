package com.originisle.android.ui.samples

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.originisle.android.MainActivity
import com.originisle.android.R
import com.originisle.android.cards.PaymentCard
import com.originisle.android.cards.SportsCard
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService

/**
 * Danh mục thẻ mẫu thử nghiệm trên Đảo Origin.
 */
object IslandSamples {

    val all: List<OriginSample> = listOf(
        OriginSample(
            "Gọi xe", "Tài xế đang đến đón · 3 phút",
            IslandPreview(R.drawable.ic_alert, "Chuyến đi", IslandPreview.Right.Capsule("3 phút")),
        ) { ctx ->
            start(ctx, 40004) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "Tài xế đang đến")
                putExtra("text", "VinFast VF8 · Trắng · 29A-888.88")
                putExtra("oi_left_content", "Chuyến đi")
                putExtra("status_chip_text", "3 phút")
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
        OriginSample(
            "Giao đồ ăn", "Tài xế đang giao đơn món · 12 phút",
            IslandPreview(R.drawable.ic_alert, "Giao hàng", IslandPreview.Right.Capsule("12 phút")),
        ) { ctx ->
            start(ctx, 40003) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "Đang giao hàng")
                putExtra("text", "Đơn hàng sắp đến nơi")
                putExtra("oi_left_content", "Giao hàng")
                putExtra("status_chip_text", "12 phút")
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
        OriginSample(
            "Biến động số dư", "Thông báo cộng tiền ngân hàng",
            IslandPreview(R.drawable.ic_payment, "+500.000 đ", IslandPreview.Right.Success),
        ) { ctx ->
            PaymentCard.post(
                context = ctx, id = 40002, appIcon = null,
                amount = "+500.000 đ", merchant = "Vietcombank", appLabel = "VCB Digibank",
                clickResp = openApp(ctx),
            )
        },
        OriginSample(
            "Điều hướng", "Chỉ đường lái xe · Quãng đường & Thời gian",
            IslandPreview(R.drawable.ic_navigation, "350 m", IslandPreview.Right.Capsule("12 phút")),
        ) { ctx ->
            start(ctx, 40011) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_DRIVING_NAVI)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "350 m")
                putExtra("text", "Đường Nguyễn Huệ")
                putExtra("oi_nav_msg", "12 phút · 4,5 km · 14:32")
                putExtra("oi_nav_assist", "Đi làn bên trái")
                putExtra("oi_left_content", "350 m")
                putExtra("status_chip_text", "12 phút")
                putExtra("icon_res", R.drawable.ic_navigation)
            }
        },
        OriginSample(
            "Tải xuống", "Thanh tiến trình tải tệp · 75%",
            IslandPreview(R.drawable.ic_download, "Đang tải", IslandPreview.Right.Progress(0.75f)),
        ) { ctx ->
            start(ctx, 40001) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS)
                putExtra("title", "Đang tải xuống")
                putExtra("text", "ban-cap-nhat-moi.apk")
                putExtra("oi_left_content", "Đang tải")
                putExtra("show_progress", true)
                putExtra("progress", 75)
                putExtra("progress_max", 100)
                putExtra("status_chip_text", "75%")
                putExtra("icon_res", R.drawable.ic_download)
            }
        },
        OriginSample(
            "Hẹn giờ", "Đồng hồ đếm ngược · 40%",
            IslandPreview(R.drawable.ic_timer, "Hẹn giờ", IslandPreview.Right.Progress(0.4f)),
        ) { ctx ->
            start(ctx, 40006) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS)
                putExtra("title", "Hẹn giờ")
                putExtra("text", "Còn lại 6:00")
                putExtra("oi_left_content", "Hẹn giờ")
                putExtra("show_progress", true)
                putExtra("progress", 40)
                putExtra("progress_max", 100)
                putExtra("status_chip_text", "6:00")
                putExtra("icon_res", R.drawable.ic_timer)
            }
        },
        OriginSample(
            "Trình phát nhạc", "Thẻ âm nhạc · Sóng âm thanh đảo",
            IslandPreview(R.drawable.ic_media_play, "Tiến Quân Ca", IslandPreview.Right.Wave),
        ) { ctx ->
            start(ctx, 40008) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE)
                putExtra("title", "Tiến Quân Ca")
                putExtra("text", "Nhạc sĩ Văn Cao")
                putExtra("oi_left_content", "Đang phát")
                putExtra("oi_wave_state", 1)
                putExtra("status_chip_text", "♪")
                putExtra("icon_res", R.drawable.ic_media_play)
            }
        },
        OriginSample(
            "Cuộc gọi đến", "Cuộc gọi thoại · Nút Từ chối / Trả lời",
            IslandPreview(R.drawable.ic_call, "", IslandPreview.Right.Text("Minh Tuấn")),
        ) { ctx ->
            start(ctx, 40009) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_show_right_icon", false)
                putExtra("title", "Minh Tuấn")
                putExtra("text", "Cuộc gọi đến")
                putExtra("oi_left_content", "")
                putExtra("oi_right_content", "Minh Tuấn")
                putStringArrayListExtra("oi_button_titles", arrayListOf("Từ chối", "Trả lời"))
                putExtra("click_resp", openApp(ctx))
                putExtra("icon_res", R.drawable.ic_call)
            }
        },
        OriginSample(
            "Tin nhắn", "Tin nhắn văn bản · Xem trước nội dung",
            IslandPreview(R.drawable.ic_avatar, "Thu Hà", IslandPreview.Right.Text("Tối nay gặp nhau nhé?")),
        ) { ctx ->
            start(ctx, 40013) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("title", "Thu Hà")
                putExtra("text", "Tối nay gặp nhau nhé?")
                putExtra("subtext", "Zalo · 2 tin nhắn mới")
                putExtra("oi_left_content", "Thu Hà")
                Icon.createWithResource(ctx, R.drawable.ic_avatar).let {
                    putExtra("oi_left_icon", it)
                    putExtra("oi_large_icon", it)
                }
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_show_right_icon", false)
                putExtra("oi_right_content", "Tối nay gặp nhau nhé?")
                putExtra("status_chip_text", "Tối nay gặp nhau nhé?")
                putExtra("icon_res", R.drawable.ic_chat)
            }
        },
        OriginSample(
            "Chuyến bay", "Thẻ thông tin lộ trình bay",
            IslandPreview(R.drawable.ic_alert, "HAN → SGN", IslandPreview.Right.Capsule("Cửa 12")),
        ) { ctx ->
            start(ctx, 40005) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_TEXT_SYMMETRY)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "VN215")
                putExtra("text", "Đang lên máy bay")
                putExtra("oi_extra1", "HAN"); putExtra("oi_extra2", "12:40")
                putExtra("oi_extra3", "SGN"); putExtra("oi_extra4", "14:55")
                putExtra("oi_left_content", "HAN → SGN")
                putExtra("status_chip_text", "Cửa 12")
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
        OriginSample(
            "Chia sẻ tệp", "Chia sẻ nhanh · Từ chối / Chấp nhận",
            IslandPreview(R.drawable.ic_alert, "Chia sẻ", IslandPreview.Right.None),
        ) { ctx ->
            start(ctx, 40010) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
                putExtra("title", "Chia sẻ lân cận")
                putExtra("text", "Thiết bị gửi một tệp hình ảnh")
                putExtra("oi_left_content", "Chia sẻ")
                putStringArrayListExtra("oi_button_titles", arrayListOf("Từ chối", "Chấp nhận"))
                putExtra("click_resp", openApp(ctx))
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
        OriginSample(
            "Tỷ số thể thao", "Bảng tỷ số bóng đá trực tiếp · 65'",
            IslandPreview(R.drawable.ic_soccer, "", IslandPreview.Right.Score(1, 1)),
        ) { ctx ->
            val ball = Icon.createWithResource(ctx, R.drawable.ic_soccer)
            SportsCard.post(
                ctx, home = "VN", homeScore = 1, away = "TL", awayScore = 1, clock = "65'",
                homeLogo = ball, awayLogo = ball,
            )
        },
        OriginSample(
            "Vé máy bay (OriginOS 7)", "Chuyến bay VN 152 · HAN ✈ SGN · Cửa 12",
            IslandPreview(R.drawable.ic_flight, "Vé bay", IslandPreview.Right.Capsule("VN 152")),
        ) { ctx ->
            start(ctx, 40011) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "VN 152 · HAN ✈ SGN")
                putExtra("text", "Giờ bay: 14:30 · Cửa: 12 · Ghế: 15A")
                putExtra("subtext", "Vé máy bay · Vietnam Airlines")
                putExtra("oi_left_content", "Vé bay")
                putExtra("oi_right_content", "VN 152")
                putExtra("status_chip_text", "VN 152")
                putExtra("icon_res", R.drawable.ic_flight)
            }
        },
        OriginSample(
            "Tai nghe TWS (OriginOS 7)", "Viễn trắc pin chi tiết · L: 95% · R: 90% · Hộp: 100%",
            IslandPreview(R.drawable.ic_earbuds, "", IslandPreview.Right.Capsule("L 95% · R 90%")),
        ) { ctx ->
            start(ctx, 40012) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "Vivo TWS 4 Pro")
                putExtra("text", "Tai Trái: 95% ⚡   Tai Phải: 90%   Hộp sạc: 100% ⚡")
                putExtra("oi_left_content", "")
                putExtra("oi_right_content", "L 95% · R 90%")
                putExtra("status_chip_text", "L 95% · R 90%")
                putExtra("oi_card_ring", 95)
                putExtra("oi_fg_color", "#FF29E683")
                putExtra("oi_bg_color", "#4C29E683")
                putExtra("icon_res", R.drawable.ic_earbuds)
            }
        },
    )

    private fun start(context: Context, id: Int, configure: Intent.() -> Unit) {
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("click_resp", openApp(context))
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
            configure()
        }
        context.startService(intent)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
