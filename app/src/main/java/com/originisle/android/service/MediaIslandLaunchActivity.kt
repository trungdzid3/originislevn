package com.originisle.android.service

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.originisle.android.cards.MediaCard

/**
 * BroadcastReceiver triggered when the user taps the small Music Island capsule.
 * Cancels the capsule notification immediately without triggering an Activity launch animation
 * in SystemUI, then displays [MediaIslandOverlay]. Falls back to opening the source music app
 * if the overlay cannot be shown.
 */
class MediaIslandLaunchReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_LAUNCH_MEDIA_OVERLAY = "com.originisle.android.ACTION_LAUNCH_MEDIA_OVERLAY"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appCtx = context.applicationContext
        if (MediaIslandOverlay.canShow(appCtx)) {
            MediaCard.setOverlaySuppression(appCtx, true)
            if (MediaIslandOverlay.show(appCtx)) {
                return
            }
            MediaCard.setOverlaySuppression(appCtx, false)
        }

        val fallbackPi = intent.getParcelableExtra(
            MediaIslandLaunchActivity.EXTRA_FALLBACK_INTENT,
            PendingIntent::class.java,
        )
        val sourcePkg = intent.getStringExtra(MediaIslandLaunchActivity.EXTRA_SOURCE_PKG)
        launchMediaFallbackApp(appCtx, fallbackPi, sourcePkg)
    }
}

internal fun launchMediaFallbackApp(
    context: Context,
    fallbackPi: PendingIntent?,
    sourcePkg: String?,
) {
    var launched = false
    if (fallbackPi != null) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val opts = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                    )
                    .toBundle()
                fallbackPi.send(context, 0, null, null, null, null, opts)
            } else {
                fallbackPi.send()
            }
            launched = true
        }
    }

    if (!launched && !sourcePkg.isNullOrBlank()) {
        val launchIntent = runCatching {
            context.packageManager.getLaunchIntentForPackage(sourcePkg)
        }.getOrNull()
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            runCatching { context.startActivity(launchIntent) }
        }
    }
}

/**
 * Translucent trampoline Activity fallback when the user taps the expanded Music Island card.
 * Attempts to display [MediaIslandOverlay] immediately; falls back to opening the source music app
 * when the overlay cannot be shown.
 */
class MediaIslandLaunchActivity : Activity() {

    companion object {
        const val EXTRA_FALLBACK_INTENT = "fallback_intent"
        const val EXTRA_SOURCE_PKG = "source_pkg"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        }

        val shown = MediaIslandOverlay.show(applicationContext)
        if (!shown) {
            val fallbackPi = intent.getParcelableExtra(EXTRA_FALLBACK_INTENT, PendingIntent::class.java)
            val sourcePkg = intent.getStringExtra(EXTRA_SOURCE_PKG)
            launchMediaFallbackApp(this, fallbackPi, sourcePkg)
        }

        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
    }
}
