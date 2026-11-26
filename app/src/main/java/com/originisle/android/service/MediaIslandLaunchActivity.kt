package com.originisle.android.service

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * Translucent trampoline Activity launched when the user taps the expanded Music Island card.
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

            var launched = false
            if (fallbackPi != null) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val opts = ActivityOptions.makeBasic()
                            .setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                            )
                            .toBundle()
                        fallbackPi.send(this, 0, null, null, null, null, opts)
                    } else {
                        fallbackPi.send()
                    }
                    launched = true
                }
            }

            if (!launched && !sourcePkg.isNullOrBlank()) {
                val launchIntent = runCatching {
                    packageManager.getLaunchIntentForPackage(sourcePkg)
                }.getOrNull()
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    runCatching { startActivity(launchIntent) }
                }
            }
        }

        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
    }
}
