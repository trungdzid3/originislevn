package com.originisle.android.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import com.originisle.android.island.PlaygroundService

class BootKeepAliveReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_CAST_NOTIFICATIONS, true)) return

        try {
            NotificationListenerService.requestRebind(
                ComponentName(context, NotificationCastListener::class.java),
            )
        } catch (_: Throwable) {
        }

        try {
            context.startForegroundService(
                Intent(context, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_KEEPALIVE),
            )
        } catch (_: Throwable) {
        }

        try {
            ShizukuKeepAliveHelper.ensureAutoAppliedAsync(context)
        } catch (_: Throwable) {
        }
    }

    companion object {
        private const val PREFS_NAME = "experimental_prefs"
        private const val KEY_CAST_NOTIFICATIONS = "cast_notifications"
    }
}
