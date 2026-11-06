package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache
import com.originisle.android.service.MediaControlReceiver
import com.originisle.android.service.NotificationCastListener
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Điều khiển và truyền phát thẻ âm nhạc lên Đảo Origin (Media Session Card).
 * Tích hợp cơ chế phát hiện Đảo gốc OriginOS 6 để nhường quyền, chống đè 2 thẻ phát nhạc.
 */
object MediaCard {

    /** Các gói dịch vụ điều khiển đảo nhạc gốc trên OriginOS 6 (Vivo / iQOO) */
    val ORIGIN_NATIVE_MUSIC_PACKAGES = setOf(
        "com.vivo.musicwidgetmix",
        "com.vivo.mediacontrol",
        "com.android.bbkmusic",
        "com.vivo.music",
    )

    private val handler = Handler(Looper.getMainLooper())
    private val pendingDismissTasks = ConcurrentHashMap<Int, Runnable>()
    private val activeMediaCastIds = ConcurrentHashMap.newKeySet<Int>()

    /**
     * Kiểm tra xem Đảo gốc OriginOS 6 có đang hiển thị trình phát nhạc hay không:
     * Chỉ trả về true khi và chỉ khi Đảo nhạc gốc của Vivo THỰC SỰ đang chạy.
     */
    fun isOriginNativePlayerActive(context: Context): Boolean {
        val listener = NotificationCastListener.instance ?: return false
        val activeNotifs = runCatching { listener.activeNotifications }.getOrNull() ?: return false
        for (active in activeNotifs) {
            val pkg = active.packageName.lowercase()
            // 1. Tiến trình widget đảo nhạc gốc của Vivo đang xuất hiện
            if (pkg in ORIGIN_NATIVE_MUSIC_PACKAGES) {
                return true
            }
            // 2. Thông báo SuperX gốc từ hệ thống với scene MUSIC / AUDIO từ gói khác
            val extras = active.notification.extras
            if (active.packageName != context.packageName &&
                (active.tag == OriginIslandConstants.SUPERX_TAG ||
                    extras?.containsKey(OriginIslandConstants.BUNDLE_KEY_SCENE) == true)
            ) {
                val scene = extras?.getString(OriginIslandConstants.BUNDLE_KEY_SCENE)?.uppercase().orEmpty()
                if (scene == "MUSIC" || scene == "AUDIO" || pkg in ORIGIN_NATIVE_MUSIC_PACKAGES) {
                    return true
                }
            }
        }
        return false
    }

    fun post(context: Context, sbn: StatusBarNotification, controller: MediaController) {
        val id = IconCache.castIdFor(sbn)
        val pkg = sbn.packageName.lowercase()

        // Khi Đảo gốc đang hiển thị nhạc,
        // Origin Isle VN chủ động thu hồi/dismiss thẻ để KHÔNG BAO GIỜ bị đè 2 thẻ nhạc trên Đảo.
        if (pkg in ORIGIN_NATIVE_MUSIC_PACKAGES || isOriginNativePlayerActive(context)) {
            clear(context, id)
            clearAll(context)
            CastLog.add("Nhạc", appLabel(context, sbn.packageName), "thu hồi thẻ — nhường Đảo gốc OriginOS 6 hiển thị", false)
            return
        }

        activeMediaCastIds.add(id)
        val n = sbn.notification
        IconCache.activeControllers[id] = controller

        val md = controller.metadata
        val track = md?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
            .ifBlank { extrasTitle(sbn) }
            .ifBlank { appLabel(context, sbn.packageName) }
            .replace("\r\n", " ")
            .replace("\n", " ")
            .trim()
            .take(60)

        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
            .ifBlank { "Đang phát nhạc" }
            .replace("\r\n", " ")
            .replace("\n", " ")
            .trim()
            .take(40)

        // Tối ưu trạng thái phát nhạc: hỗ trợ các app không gửi PlaybackState đầy đủ
        val playbackStateValue = controller.playbackState?.state
        val playing = when (playbackStateValue) {
            PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_FAST_FORWARDING, PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT, PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_CONNECTING,
            -> true
            null -> sbn.isOngoing
            else -> false
        }

        val notifLargeIcon = n.getLargeIcon()
        val notifLargeBitmap = when (val large = n.extras.get(NotificationCompat.EXTRA_LARGE_ICON)) {
            is Bitmap -> large
            else -> notifLargeIcon?.loadDrawable(context)?.let { d ->
                val w = if (d.intrinsicWidth > 0) d.intrinsicWidth.coerceIn(48, 200) else 120
                val h = if (d.intrinsicHeight > 0) d.intrinsicHeight.coerceIn(48, 200) else 120
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val cv = Canvas(bmp)
                d.setBounds(0, 0, w, h)
                d.draw(cv)
                bmp
            }
        }
        val albumArt: Bitmap? = (md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: notifLargeBitmap)?.let(::shrinkForIcon)

        IconCache.activeSmallIcons[id] = IconCache.safeAppIcon(context, sbn.packageName)
        val displayIcon: Icon = if (albumArt != null) {
            IconCache.activeLargeBitmaps[id] = albumArt
            Icon.createWithBitmap(albumArt)
        } else {
            IconCache.safeAppIcon(context, sbn.packageName)
        }
        val waveColors = waveColorsFrom(albumArt)

        val showChrono = n.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER, false)
        val chip = if (showChrono && n.`when` > 0) {
            formatElapsed(abs(System.currentTimeMillis() - n.`when`) / 1000)
        } else if (playing) {
            "Đang phát"
        } else {
            "Tạm dừng"
        }

        fun mediaAction(action: String) = PendingIntent.getBroadcast(
            context, (id.toString() + action).hashCode(),
            Intent(action).setPackage(context.packageName)
                .putExtra("cast_id", id)
                .putExtra(MediaControlReceiver.EXTRA_TOKEN, controller.sessionToken),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        fun findSourceAction(titleKeywords: List<String>): PendingIntent? =
            n.actions?.firstOrNull { a ->
                val title = a.title?.toString()?.lowercase().orEmpty()
                titleKeywords.any { title.contains(it) }
            }?.actionIntent

        val prevSourceAction = findSourceAction(listOf("previous", "skip back", "rewind"))
        val nextSourceAction = findSourceAction(listOf("next", "skip forward", "fast forward"))

        val actions = arrayListOf<Notification.Action>(
            makeAction(
                context,
                R.drawable.ic_media_prev,
                prevSourceAction ?: mediaAction(MediaControlReceiver.ACTION_PREV),
            ),
            makeAction(
                context,
                if (playing) R.drawable.ic_media_pause else R.drawable.ic_media_play,
                mediaAction(MediaControlReceiver.ACTION_PLAY_PAUSE),
            ),
            makeAction(
                context,
                R.drawable.ic_media_next,
                nextSourceAction ?: mediaAction(MediaControlReceiver.ACTION_NEXT),
            ),
        )

        if (playing) {
            pendingDismissTasks.remove(id)?.let { handler.removeCallbacks(it) }
        } else {
            pendingDismissTasks.remove(id)?.let { handler.removeCallbacks(it) }
            val task = Runnable {
                pendingDismissTasks.remove(id)
                val cancelIntent = Intent(context, PlaygroundService::class.java).apply {
                    action = PlaygroundService.ACTION_CANCEL
                    putExtra("id", id)
                    putExtra("oi_scene", "NAVIGATION")
                }
                context.startService(cancelIntent)
            }
            pendingDismissTasks[id] = task
            handler.postDelayed(task, 3000L)
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", playing)

            putExtra("oi_scene", "NAVIGATION")
            putExtra("title", track)
            putExtra("text", artist)
            putExtra("source_pkg", sbn.packageName)
            putExtra("oi_left_content", if (albumArt != null) "" else track.take(10))

            putExtra("status_chip_text", chip)

            putExtra("oi_left_icon", displayIcon)
            putExtra("oi_large_icon", displayIcon)
            putExtra("oi_force_show", true)
            putExtra("icon_res", R.drawable.ic_media_play)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
            putExtra("oi_fg_color", "#FFFFFFFF")

            if (playing) {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE)
                putExtra("oi_wave_state", 1)
                if (waveColors.isNotEmpty()) {
                    putStringArrayListExtra("oi_wave_color_list", ArrayList(waveColors))
                }
            } else {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_right_icon", IconCache.safeResourceIcon(context, R.drawable.ic_media_pause))
                putExtra("oi_show_right_icon", true)
            }

            putParcelableArrayListExtra("actions", actions)
            putExtra(
                "source_content_intent",
                n.contentIntent ?: fallbackClickIntent(context, sbn.packageName),
            )
        }
        context.startService(intent)
        CastLog.add("Nhạc", track, "hiển thị — $track ($artist)", true)
    }

    fun clearAll(context: Context) {
        val ids = activeMediaCastIds.toList()
        activeMediaCastIds.clear()
        for (id in ids) {
            clear(context, id)
        }
    }

    fun clear(context: Context, id: Int) {
        activeMediaCastIds.remove(id)
        pendingDismissTasks.remove(id)?.let { handler.removeCallbacks(it) }
        IconCache.clear(id)
        val cancelIntent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_CANCEL
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
        }
        context.startService(cancelIntent)
    }

    private fun extrasTitle(sbn: StatusBarNotification): String =
        sbn.notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()

    private fun appLabel(context: Context, pkg: String): String = runCatching {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun makeAction(context: Context, iconRes: Int, pi: PendingIntent): Notification.Action =
        Notification.Action.Builder(IconCache.safeResourceIcon(context, iconRes), "", pi).build()

    /**
     * Trích xuất dải màu sắc trực tiếp từ ảnh bìa album để phục vụ sóng âm visualiser (island.superx.rightInfo.waveColor)
     */
    private fun waveColorsFrom(bitmap: Bitmap?): List<String> {
        if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) return emptyList()
        if (bitmap.config == Bitmap.Config.HARDWARE) return emptyList()
        return runCatching {
            val w = bitmap.width
            val h = bitmap.height
            listOf(0.25f to 0.5f, 0.5f to 0.3f, 0.75f to 0.5f).map { (fx, fy) ->
                val x = (fx * (w - 1)).toInt().coerceIn(0, w - 1)
                val y = (fy * (h - 1)).toInt().coerceIn(0, h - 1)
                String.format("#%06X", 0xFFFFFF and bitmap.getPixel(x, y))
            }
        }.getOrDefault(emptyList())
    }
}
