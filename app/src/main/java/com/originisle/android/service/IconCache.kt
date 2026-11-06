package com.originisle.android.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.media.session.MediaController
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import com.originisle.android.R
import com.originisle.android.cards.QuickShareCard
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared per-cast-id state, written by the card posters in `cards/` (as they inspect a source
 * notification) and read by [com.originisle.android.island.PlaygroundService] (when it builds the
 * SuperX bundle) and [MediaControlReceiver] (to dispatch transport controls back to the right
 * [MediaController]). Centralising this avoids the card posters and the service depending on each
 * other just to share these maps.
 */
object IconCache {
    val activeControllers = ConcurrentHashMap<Int, MediaController>()
    val activeSmallIcons = ConcurrentHashMap<Int, Icon>()
    val activeLargeIcons = ConcurrentHashMap<Int, Icon>()
    val activeLargeBitmaps = ConcurrentHashMap<Int, Bitmap>()

    /** Deterministic, stable cast id per notification key (band 20000-29999). */
    fun castIdFor(sbn: StatusBarNotification): Int =
        (sbn.key.hashCode() and Int.MAX_VALUE) % 10000 + 20000

    /** Drop everything cached for a card id once its source notification is gone. */
    fun clear(id: Int) {
        activeControllers.remove(id)
        activeSmallIcons.remove(id)
        activeLargeIcons.remove(id)
        activeLargeBitmaps.remove(id)
    }

    private fun createFallbackLauncherBitmapIcon(context: Context): Icon {
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val drawable = runCatching {
            ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
        }.getOrNull()
        if (drawable != null) {
            drawable.setBounds(0, 0, 96, 96)
            drawable.draw(canvas)
        } else {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D111A")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(48f, 48f, 48f, paint)
        }
        return Icon.createWithBitmap(bitmap)
    }

    /**
     * Safely resolve an application's icon as a standalone pure Bitmap Icon.
     * Prevents Vivo SuperX / OriginOS SystemUI from throwing cross-package
     * Resources$NotFoundException or SecurityException when unparceling notifications.
     */
    fun safeAppIcon(context: Context, packageName: String): Icon {
        if (packageName == "com.google.android.gms" || packageName.startsWith("com.google.android.gms")) {
            return QuickShareCard.createQuickShareOfficialIcon(context)
        }
        return runCatching {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceIn(48, 144) else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceIn(48, 144) else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            Icon.createWithBitmap(bitmap)
        }.getOrElse {
            createFallbackLauncherBitmapIcon(context)
        }
    }

    /**
     * Safely resolve any drawable resource (including Vector XML drawables) as a standalone
     * pure Bitmap Icon. On Vivo OriginOS, passing raw resource-backed Icons (R.drawable.xxx)
     * into SuperX bundles causes SystemUI to fail inflating remote VectorDrawables, which triggers
     * SUPERX_VALUE = false and immediately drops the card off the island.
     */
    fun safeResourceIcon(context: Context, resId: Int): Icon {
        if (resId == 0 || resId == R.mipmap.ic_launcher_round) {
            return createFallbackLauncherBitmapIcon(context)
        }
        return runCatching {
            val drawable = ContextCompat.getDrawable(context, resId)
                ?: return createFallbackLauncherBitmapIcon(context)
            val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, 96, 96)
            drawable.draw(canvas)
            Icon.createWithBitmap(bitmap)
        }.getOrElse {
            createFallbackLauncherBitmapIcon(context)
        }
    }
}
