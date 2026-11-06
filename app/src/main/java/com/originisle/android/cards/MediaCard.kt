package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
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

    /** Các gói ứng dụng âm nhạc bên thứ ba phổ biến không bao giờ bị thu hồi nhầm */
    val THIRD_PARTY_MUSIC_PACKAGES = setOf(
        "com.zing.mp3",
        "com.spotify.music",
        "com.spotify.music.lite",
        "com.nhaccuatui.player",
        "com.google.android.apps.youtube.music",
        "com.soundcloud.android",
        "com.apple.android.music",
        "deezer.android.app",
        "com.amazon.mp3",
    )

    private val handler = Handler(Looper.getMainLooper())
    private val pendingDismissTasks = ConcurrentHashMap<Int, Runnable>()
    private val activeMediaCastIds = ConcurrentHashMap.newKeySet<Int>()

    /**
     * Kiểm tra xem Đảo gốc OriginOS 6 có đang hiển thị trình phát nhạc hay không:
     * Chỉ trả về true khi và chỉ khi Đảo nhạc gốc của Vivo THỰC SỰ đang chạy và đang ongoing.
     * Tuyệt đối không tính các ứng dụng nhạc ngoài như Zing MP3, Spotify, Nhaccuatui, YouTube Music.
     */
    fun isOriginNativePlayerActive(context: Context): Boolean {
        val listener = NotificationCastListener.instance ?: return false
        val activeNotifs = runCatching { listener.activeNotifications }.getOrNull() ?: return false
        for (active in activeNotifs) {
            val pkg = active.packageName.lowercase()
            if (pkg == context.packageName.lowercase()) continue
            if (pkg in THIRD_PARTY_MUSIC_PACKAGES) continue

            val isOngoing = active.isOngoing || (active.notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
            if (!isOngoing) continue

            // 1. Tiến trình widget đảo nhạc gốc của Vivo đang xuất hiện VÀ thực sự đang ongoing
            if (pkg in ORIGIN_NATIVE_MUSIC_PACKAGES) {
                return true
            }
            // 2. Thông báo SuperX gốc từ hệ thống với scene MUSIC / AUDIO từ gói khác VÀ đang ongoing
            val extras = active.notification.extras
            if (active.tag == OriginIslandConstants.SUPERX_TAG ||
                extras?.containsKey(OriginIslandConstants.BUNDLE_KEY_SCENE) == true
            ) {
                val scene = extras?.getString(OriginIslandConstants.BUNDLE_KEY_SCENE)?.uppercase().orEmpty()
                if (scene == "MUSIC" || scene == "AUDIO") {
                    return true
                }
            }
        }
        return false
    }

    fun post(context: Context, sbn: StatusBarNotification, controller: MediaController) {
        val id = IconCache.castIdFor(sbn)
        val pkg = sbn.packageName.lowercase()

        // 1. Nếu chính gói phát nhạc gốc của Vivo gửi thông báo: nhường cho Đảo gốc, không can thiệp
        if (pkg in ORIGIN_NATIVE_MUSIC_PACKAGES) {
            clear(context, id)
            return
        }

        // 2. Tuyệt đối KHÔNG thu hồi thẻ của các ứng dụng nhạc ngoài (Zing MP3, Spotify, Nhaccuatui, YouTube Music, ...)
        val isThirdParty = pkg in THIRD_PARTY_MUSIC_PACKAGES ||
            (!pkg.startsWith("com.vivo.") && !pkg.startsWith("com.bbk.") && !pkg.startsWith("com.android.bbk"))

        if (!isThirdParty && isOriginNativePlayerActive(context)) {
            clear(context, id)
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

        val durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val durSec = if (durationMs > 0) durationMs / 1000 else 0L
        val posMs = controller.playbackState?.position ?: 0L
        val posSec = if (posMs > 0) posMs / 1000 else 0L

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

        val subtext = when {
            durationMs > 0 && posSec > 0 -> "$artist • ${formatElapsed(posSec)} / ${formatElapsed(durSec)}"
            durationMs > 0 -> "$artist • ${formatElapsed(durSec)}"
            playing -> "$artist • Đang phát"
            else -> "$artist • Tạm dừng"
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
            ?: notifLargeBitmap)?.let(::shrinkForIcon)?.let(::roundCorners)

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
        } else if (durationMs > 0 && posSec > 0) {
            "${formatElapsed(posSec)} / ${formatElapsed(durSec)}"
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
            putExtra("text", subtext)
            putExtra("subtext", subtext)
            putExtra("source_pkg", sbn.packageName)
            putExtra("oi_left_content", if (albumArt != null) "" else track.take(10))

            putExtra("oi_left_icon", displayIcon)
            putExtra("oi_large_icon", displayIcon)
            putExtra("oi_show_left_icon", true)
            putExtra("oi_force_show", true)
            putExtra("icon_res", R.drawable.ic_media_play)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
            putExtra("oi_fg_color", "#FFFFFFFF")
            putExtra("oi_bg_color", "#FF262626")

            if (playing) {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE)
                putExtra("oi_wave_state", 1)
                putStringArrayListExtra("oi_wave_color_list", ArrayList(waveColors))

                putExtra("oi_right_icon", IconCache.safeResourceIcon(context, R.drawable.ic_media_play))
                putExtra("oi_show_right_icon", true)
                putExtra("status_chip_text", chip)
                putExtra("oi_right_content", if (chip.isNotBlank()) chip else "♪")
            } else {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_right_icon", IconCache.safeResourceIcon(context, R.drawable.ic_media_pause))
                putExtra("oi_show_right_icon", true)
                putExtra("status_chip_text", "Tạm dừng")
                putExtra("oi_right_content", "Tạm dừng")
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
     * Bo góc cho ảnh bìa album (squircle bo cong mềm mại theo chuẩn thiết kế OriginOS 6)
     */
    private fun roundCorners(bitmap: Bitmap, cornerRadiusRatio: Float = 0.22f): Bitmap {
        return runCatching {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return bitmap
            val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
            val radius = minOf(w, h) * cornerRadiusRatio
            canvas.drawRoundRect(rect, radius, radius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            output
        }.getOrDefault(bitmap)
    }

    /**
     * Trích xuất dải màu sắc trực tiếp từ ảnh bìa album để phục vụ sóng âm visualiser (island.superx.rightInfo.waveColor)
     */
    private fun waveColorsFrom(bitmap: Bitmap?): List<String> {
        val fallback = listOf("#FF5376", "#00E5FF", "#7C4DFF")
        if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) return fallback
        return runCatching {
            val src = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return fallback
            } else {
                bitmap
            }
            try {
                val w = src.width
                val h = src.height
                val hsv = FloatArray(3)
                val samplePoints = listOf(0.25f to 0.5f, 0.5f to 0.35f, 0.75f to 0.5f)
                val colors = samplePoints.map { (fx, fy) ->
                    val x = (fx * (w - 1)).toInt().coerceIn(0, w - 1)
                    val y = (fy * (h - 1)).toInt().coerceIn(0, h - 1)
                    val pixel = src.getPixel(x, y)
                    Color.colorToHSV(pixel, hsv)
                    if (hsv[1] < 0.45f) hsv[1] = 0.65f
                    if (hsv[2] < 0.65f) hsv[2] = 0.90f
                    val adjusted = Color.HSVToColor(hsv)
                    String.format("#%06X", 0xFFFFFF and adjusted)
                }
                if (colors.size >= 3) colors.take(3) else fallback
            } finally {
                if (src != bitmap) {
                    src.recycle()
                }
            }
        }.getOrDefault(fallback)
    }
}
