package com.originisle.android.smart

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.CopyCodeReceiver

object ScreenshotCard {

    private const val CARD_ID = 35001 // ID riêng cố định cho Thẻ thông minh Ảnh chụp màn hình OriginOS 7
    private const val HOLD_MS = 180_000L // Giữ trên Đảo 3 phút để người dùng lấy mã thuận tiện

    private val handler = Handler(Looper.getMainLooper())

    fun post(context: Context, info: SmartInfo) {
        val app = context.applicationContext

        val iconRes = when (info.type) {
            SmartCardType.FLIGHT -> R.drawable.ic_flight
            SmartCardType.DELIVERY -> R.drawable.ic_package
            SmartCardType.TICKET -> R.drawable.ic_ticket
        }

        val leftLabel = when (info.type) {
            SmartCardType.FLIGHT -> "Chuyến bay"
            SmartCardType.DELIVERY -> "Mã nhận"
            SmartCardType.TICKET -> "Vé"
        }

        // Action 1: Sao chép mã vào clipboard
        val copyIntent = PendingIntent.getBroadcast(
            app, CARD_ID,
            Intent(app, CopyCodeReceiver::class.java)
                .putExtra(CopyCodeReceiver.EXTRA_CODE, info.code)
                .putExtra("cast_id", CARD_ID),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val copyAction = Notification.Action.Builder(
            Icon.createWithResource(app, R.mipmap.ic_launcher_round), "Sao chép mã", copyIntent
        ).build()

        val actionsList = arrayListOf(copyAction)

        // Action 2: Thêm vào Google Calendar / Lịch hệ thống nếu là chuyến bay hoặc có ngày giờ
        if (info.type == SmartCardType.FLIGHT || info.timeString != null) {
            val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, info.title)
                putExtra(CalendarContract.Events.DESCRIPTION, info.detail)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val calendarPi = PendingIntent.getActivity(
                app, CARD_ID + 1, calendarIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actionsList.add(
                Notification.Action.Builder(
                    Icon.createWithResource(app, R.mipmap.ic_launcher_round), "Thêm vào lịch", calendarPi
                ).build()
            )
        }

        // Tapping vào card: Mở xem ảnh chụp màn hình trong Thư viện ảnh (Gallery)
        val clickIntent = if (info.sourceUri != null) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(info.sourceUri, "image/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(app, com.originisle.android.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        val clickPi = PendingIntent.getActivity(
            app, CARD_ID + 2, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intent = Intent(app, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", CARD_ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
            putExtra("title", info.title)
            putExtra("text", info.detail)
            putExtra("subtext", info.subtitle)
            putExtra("source_app", "Ảnh chụp màn hình AI")
            putExtra("oi_left_content", leftLabel)
            putExtra("oi_right_content", info.code)
            putExtra("status_chip_text", info.code)
            putExtra("icon_res", iconRes)
            putParcelableArrayListExtra("actions", actionsList)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
        }

        app.startService(intent)

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ cancel(app) }, HOLD_MS)
    }

    fun cancel(context: Context) {
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", CARD_ID)
                .putExtra("oi_scene", "NAVIGATION")
        )
    }
}
