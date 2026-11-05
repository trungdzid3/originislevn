package com.originisle.android.cards

import android.content.Context
import android.content.Intent
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.ride.RideInfo
import com.originisle.android.ride.RideType

object RideCard {

    fun post(context: Context, info: RideInfo, notificationId: Int) {
        val scene = if (info.type == RideType.TAXI) "TAXI" else "DELIEVERY"

        val capsuleContent = buildString {
            if (!info.licensePlate.isNullOrBlank()) {
                append(info.licensePlate)
            } else {
                append(info.appName)
            }
            if (info.etaMinutes != null) {
                append(" • ${info.etaMinutes}p")
            }
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", notificationId)
            putExtra("oi_scene", scene)
            putExtra("title", info.title)
            putExtra("text", info.statusText)
            putExtra("source_pkg", info.packageName)
            putExtra("is_ongoing", true)

            // SuperX configuration
            putExtra("oi_left_content", info.appName)
            putExtra("oi_bg_color", "#FF1C1C1E")
            putExtra("oi_fg_color", "#FFFFFFFF")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_NAVIGATION)
            putExtra("status_chip_text", capsuleContent)
        }

        context.startService(intent)
    }
}
