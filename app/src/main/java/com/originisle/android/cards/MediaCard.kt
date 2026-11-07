package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache
import com.originisle.android.service.MediaControlReceiver
import com.originisle.android.service.NotificationCastListener
import java.util.concurrent.ConcurrentHashMap

/**
 * Điều khiển và truyền phát thẻ âm nhạc lên Đảo Origin (Media Session Card).
 * Tích hợp cơ chế phát hiện Đảo gốc OriginOS 6 để nhường quyền, chống đè 2 thẻ phát nhạc.
 */
object MediaCard {

    const val ID = 35010

    @Volatile private var lastTrack: String = ""
    @Volatile private var lastArtist: String = ""
    @Volatile private var lastPlaying: Boolean? = null
    @Volatile private var lastPkg: String = ""
    @Volatile private var lastUiNightMode: Boolean? = null
    @Volatile private var lastArtSignature: String = ""
    @Volatile private var lastDispatchTimeMs: Long = 0L

    /** Các gói dịch vụ điều khiển đảo nhạc gốc trên OriginOS 6 (Vivo / iQOO) */
    val ORIGIN_NATIVE_MUSIC_PACKAGES = setOf(
        "com.vivo.musicwidgetmix",
        "com.vivo.mediacontrol",
        "com.android.bbkmusic",
        "com.vivo.music",
    )

    /** Các gói ứng dụng âm nhạc bên thứ ba phổ biến không bao giờ bị thu hồi nhầm */
    val THIRD_PARTY_MUSIC_PACKAGES = setOf(
        "com.spotify.music",
        "com.spotify.music.lite",
        "com.spotify.music.canary",
        "com.google.android.apps.youtube.music",
        "com.soundcloud.android",
        "com.apple.android.music",
        "deezer.android.app",
        "com.amazon.mp3",
        "com.aspiro.tidal",
        "com.audiomack",
        "tunein.player",
        "fm.player",
        "vstudio.android.musicplayer",
    )

    private val handler = Handler(Looper.getMainLooper())
    private val pendingDismissTasks = ConcurrentHashMap<Int, Runnable>()
    private val activeMediaCastIds = ConcurrentHashMap.newKeySet<Int>()

    /**
     * Kiểm tra xem Đảo gốc OriginOS 6 có đang hiển thị trình phát nhạc hay không:
     * Chỉ trả về true khi và chỉ khi Đảo nhạc gốc của Vivo THỰC SỰ đang chạy và đang ongoing.
     * Tuyệt đối không tính các ứng dụng nhạc ngoài như Spotify, Apple Music, YouTube Music.
     */
    fun isOriginNativePlayerActive(context: Context): Boolean {
        val listener = NotificationCastListener.instance ?: return false
        val activeNotifs = runCatching { listener.activeNotifications }.getOrNull() ?: return false
        for (active in activeNotifs) {
            val pkg = active.packageName.lowercase()
            if (pkg == context.packageName.lowercase()) continue
            if (pkg in THIRD_PARTY_MUSIC_PACKAGES || NotificationCastListener.isKnownMusicApp(pkg)) continue

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

    fun post(
        context: Context,
        sbn: StatusBarNotification,
        controller: MediaController?,
        forceRefresh: Boolean = false,
    ) {
        val legacyId = IconCache.castIdFor(sbn)
        if (legacyId != ID && PlaygroundService.activeCardIds.contains(legacyId)) {
            clear(context, legacyId)
        }

        val pkg = sbn.packageName.lowercase()

        // 1. Nếu chính gói phát nhạc gốc của Vivo gửi thông báo: nhường cho Đảo gốc, không can thiệp
        if (pkg in ORIGIN_NATIVE_MUSIC_PACKAGES) {
            clear(context, ID)
            return
        }

        // 2. Tuyệt đối KHÔNG thu hồi thẻ của các ứng dụng nhạc ngoài (Spotify, Apple Music, YouTube Music, ...)
        val isThirdParty = pkg in THIRD_PARTY_MUSIC_PACKAGES ||
            (!pkg.startsWith("com.vivo.") && !pkg.startsWith("com.bbk.") && !pkg.startsWith("com.android.bbk"))

        if (!isThirdParty && isOriginNativePlayerActive(context)) {
            clear(context, ID)
            CastLog.add("Nhạc", appLabel(context, sbn.packageName), "thu hồi thẻ — nhường Đảo gốc OriginOS 6 hiển thị", false)
            return
        }

        val n = sbn.notification
        val md = controller?.metadata
        val track = md?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
            .ifBlank { extrasTitle(sbn) }
            .ifBlank { appLabel(context, sbn.packageName) }
            .replace("\r\n", " ")
            .replace("\n", " ")
            .trim()
            .take(60)

        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
            .ifBlank {
                n.extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
            }
            .ifBlank { "Đang phát nhạc" }
            .replace("\r\n", " ")
            .replace("\n", " ")
            .trim()
            .take(40)

        // Tối ưu trạng thái phát nhạc: hỗ trợ các app không gửi PlaybackState đầy đủ
        val playbackStateValue = controller?.playbackState?.state
        val playing = when (playbackStateValue) {
            PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_FAST_FORWARDING, PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT, PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_CONNECTING,
            -> true
            null -> sbn.isOngoing
            else -> false
        }

        if (controller != null) {
            IconCache.activeControllers[legacyId] = controller
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
        val rawAlbumArt: Bitmap? = (md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: notifLargeBitmap)?.let(::shrinkForIcon)

        val durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val positionMs = (controller?.playbackState?.position ?: 0L).coerceAtLeast(0L)

        dispatchMediaCard(
            context = context,
            pkg = sbn.packageName,
            controller = controller,
            track = track,
            artist = artist,
            playing = playing,
            rawAlbumArt = rawAlbumArt,
            durationMs = durationMs,
            positionMs = positionMs,
            sourceActions = n.actions,
            contentIntent = n.contentIntent,
            forceRefresh = forceRefresh,
        )
    }

    fun postFromController(
        ctx: Context,
        pkg: String,
        controller: MediaController,
        forceRefresh: Boolean = false,
    ) {
        val pkgLower = pkg.lowercase()
        if (pkgLower in ORIGIN_NATIVE_MUSIC_PACKAGES) {
            clear(ctx, ID)
            return
        }

        val isThirdParty = pkgLower in THIRD_PARTY_MUSIC_PACKAGES ||
            (!pkgLower.startsWith("com.vivo.") && !pkgLower.startsWith("com.bbk.") && !pkgLower.startsWith("com.android.bbk"))

        if (!isThirdParty && isOriginNativePlayerActive(ctx)) {
            clear(ctx, ID)
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
            .ifBlank { metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.trim().orEmpty() }
            .ifBlank { appLabel(ctx, pkg) }
            .replace("\r\n", " ")
            .replace("\n", " ")
            .trim()
            .take(60)

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
            .ifBlank { metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.trim().orEmpty() }
            .ifBlank { metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.trim().orEmpty() }
            .ifBlank { "Đang phát nhạc" }
            .replace("\r\n", " ")
            .replace("\n", " ")
            .trim()
            .take(40)

        val art: Bitmap? = (metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON))?.let(::shrinkForIcon)

        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val positionMs = (playbackState?.position ?: 0L).coerceAtLeast(0L)
        val playing = when (playbackState?.state) {
            PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_FAST_FORWARDING, PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT, PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_CONNECTING,
            -> true
            else -> false
        }

        dispatchMediaCard(
            context = ctx,
            pkg = pkg,
            controller = controller,
            track = title,
            artist = artist,
            playing = playing,
            rawAlbumArt = art,
            durationMs = durationMs,
            positionMs = positionMs,
            sourceActions = null,
            contentIntent = controller.sessionActivity,
            forceRefresh = forceRefresh,
        )
    }

    private fun dispatchMediaCard(
        context: Context,
        pkg: String,
        controller: MediaController?,
        track: String,
        artist: String,
        playing: Boolean,
        rawAlbumArt: Bitmap?,
        durationMs: Long,
        positionMs: Long,
        sourceActions: Array<Notification.Action>?,
        contentIntent: PendingIntent?,
        forceRefresh: Boolean,
    ) {
        val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        activeMediaCastIds.add(ID)
        if (controller != null) {
            IconCache.activeControllers[ID] = controller
        }

        val artSignature = rawAlbumArt?.let { bmp ->
            val soft = ensureSoftwareBitmap(bmp)
            if (soft.width > 0 && soft.height > 0) "${soft.width}x${soft.height}:${soft.getPixel(soft.width / 2, soft.height / 2)}" else "empty"
        } ?: "none"

        val now = SystemClock.elapsedRealtime()
        val stateUnchanged = (
            track == lastTrack &&
                artist == lastArtist &&
                playing == lastPlaying &&
                pkg == lastPkg &&
                isNightMode == lastUiNightMode &&
                artSignature == lastArtSignature
            )
        if (stateUnchanged && !forceRefresh && PlaygroundService.activeCardIds.contains(ID) && (now - lastDispatchTimeMs) < 3000L) {
            return
        }
        lastTrack = track
        lastArtist = artist
        lastPlaying = playing
        lastPkg = pkg
        lastUiNightMode = isNightMode
        lastArtSignature = artSignature
        lastDispatchTimeMs = now

        val durSec = if (durationMs > 0L) durationMs / 1000L else 0L
        val posSec = if (positionMs > 0L) positionMs / 1000L else 0L
        val subtext = if (durationMs > 0L) {
            "${formatElapsed(posSec)} / ${formatElapsed(durSec)}"
        } else {
            appLabel(context, pkg)
        }

        val baseAlbumBitmap: Bitmap = rawAlbumArt ?: createDefaultAlbumBitmap(context, pkg)
        val artBmp: Bitmap = roundCorners(baseAlbumBitmap, 0.20f)
        val expandedArtBmp: Bitmap = composeAlbumArtWithAppBadge(context, artBmp, pkg)

        val pillLeftIcon: Icon = Icon.createWithBitmap(artBmp)
        val badgedAlbumIcon: Icon = Icon.createWithBitmap(expandedArtBmp)

        IconCache.activeSmallIcons[ID] = pillLeftIcon
        IconCache.activeLargeBitmaps[ID] = expandedArtBmp

        val waveColors = ArrayList(waveColorsFrom(rawAlbumArt ?: baseAlbumBitmap))

        fun mediaAction(action: String) = PendingIntent.getBroadcast(
            context, (ID.toString() + action).hashCode(),
            Intent(action).setPackage(context.packageName)
                .putExtra("cast_id", ID)
                .apply {
                    controller?.sessionToken?.let { putExtra(MediaControlReceiver.EXTRA_TOKEN, it) }
                },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        fun findSourceAction(titleKeywords: List<String>): PendingIntent? =
            sourceActions?.firstOrNull { a ->
                val title = a.title?.toString()?.lowercase().orEmpty()
                titleKeywords.any { title.contains(it) }
            }?.actionIntent

        val prevAction = findSourceAction(listOf("previous", "skip back", "rewind", "trước", "lùi"))
            ?: mediaAction(MediaControlReceiver.ACTION_PREV)
        val playPauseAction = mediaAction(MediaControlReceiver.ACTION_PLAY_PAUSE)
        val nextAction = findSourceAction(listOf("next", "skip forward", "fast forward", "tiếp", "tới"))
            ?: mediaAction(MediaControlReceiver.ACTION_NEXT)

        val contentClickIntent: PendingIntent = contentIntent ?: fallbackClickIntent(context, pkg)

        val actions = arrayListOf<Notification.Action>(
            makeAction(context, R.drawable.ic_media_prev, prevAction),
            makeAction(
                context,
                if (playing) R.drawable.ic_media_pause else R.drawable.ic_media_play,
                playPauseAction,
            ),
            makeAction(context, R.drawable.ic_media_next, nextAction),
        )

        val buttonIcons = arrayListOf<Icon>(
            actions[0].getIcon() ?: IconCache.safeResourceIcon(context, R.drawable.ic_media_prev),
            actions[1].getIcon() ?: IconCache.safeResourceIcon(context, if (playing) R.drawable.ic_media_pause else R.drawable.ic_media_play),
            actions[2].getIcon() ?: IconCache.safeResourceIcon(context, R.drawable.ic_media_next),
        )
        val buttonActions = arrayListOf<PendingIntent>(prevAction, playPauseAction, nextAction)

        pendingDismissTasks.remove(ID)?.let { handler.removeCallbacks(it) }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("template", OriginIslandConstants.TEMPLATE_BUTTONS)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
            putExtra("scene", "NAVIGATION")
            putExtra("oi_scene", "NAVIGATION")
            putExtra("title", track)
            putExtra("content", artist)
            putExtra("text", artist)
            putExtra("subtext", subtext)
            putExtra("critical", true)
            putExtra("oi_ongoing", playing)
            putExtra("is_ongoing", playing)
            putExtra("oi_auto_Disappear", false)
            putExtra("oi_island_click_expand", true)
            putExtra("oi_force_show", true)
            putExtra("oi_show_notify", false)
            putExtra("oi_Suppress_Toast", true)

            putExtra("source_pkg", pkg)
            putExtra("oi_left_content", "")
            putExtra("oi_left_icon", pillLeftIcon)
            putExtra("oi_large_icon", badgedAlbumIcon)
            putExtra("oi_show_left_icon", true)
            putExtra("icon_res", R.drawable.ic_media_play)
            putStringArrayListExtra("oi_button_titles", arrayListOf("", "", ""))
            putParcelableArrayListExtra("oi_button_actions", buttonActions)
            putParcelableArrayListExtra("oi_button_icons", buttonIcons)

            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE)
            putExtra("oi_wave_state", if (playing) 1 else 0)
            putStringArrayListExtra("oi_wave_colors", waveColors)
            putStringArrayListExtra("oi_wave_color_list", waveColors)
            putExtra("oi_show_right_icon", false)
            putExtra("status_chip_text", "")
            putExtra("oi_right_content", "")

            putParcelableArrayListExtra("actions", actions)
            putExtra(
                "source_content_intent",
                contentClickIntent,
            )
        }
        context.startService(intent)
        CastLog.add("Nhạc", track, "hiển thị — $track ($artist)", true)
    }

    fun clearAll(context: Context) {
        val ids = (activeMediaCastIds + ID).toList()
        activeMediaCastIds.clear()
        for (id in ids) {
            clear(context, id)
        }
    }

    fun clear(context: Context, id: Int = ID) {
        if (id == ID) {
            lastTrack = ""
            lastArtist = ""
            lastPlaying = null
            lastPkg = ""
            lastUiNightMode = null
            lastArtSignature = ""
            lastDispatchTimeMs = 0L
        }
        activeMediaCastIds.remove(id)
        pendingDismissTasks.remove(id)?.let { handler.removeCallbacks(it) }
        IconCache.clear(id)
        val cancelIntent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_CANCEL
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
        }
        context.startService(cancelIntent)

        // Nếu nơi gọi truyền legacyId (từ sbn) khi thông báo nhạc bị gỡ và legacyId không phải thẻ riêng đang chạy,
        // đồng thời giải phóng luôn thẻ nhạc chuẩn ID = 35010
        if (id != ID && !PlaygroundService.activeCardIds.contains(id) && PlaygroundService.activeCardIds.contains(ID)) {
            clear(context, ID)
        }
    }

    private fun extrasTitle(sbn: StatusBarNotification): String =
        sbn.notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()

    private fun appLabel(context: Context, pkg: String): String = runCatching {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun renderTintedBitmapIcon(context: Context, iconRes: Int, tintColor: Int): Icon? = runCatching {
        val drawable = ContextCompat.getDrawable(context, iconRes)?.mutate() ?: return null
        val size = 96
        val pad = 16
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(pad, pad, size - pad, size - pad)
        drawable.setTint(tintColor)
        drawable.setTintMode(PorterDuff.Mode.SRC_IN)
        drawable.draw(canvas)
        Icon.createWithBitmap(bmp)
    }.getOrNull()

    private fun makeAction(context: Context, iconRes: Int, pi: PendingIntent): Notification.Action {
        val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val actionIconColor = if (isNightMode) Color.WHITE else Color.parseColor("#FF1C1C1E")
        val icon = renderTintedBitmapIcon(context, iconRes, actionIconColor)
            ?: IconCache.safeResourceIcon(context, iconRes)
        return Notification.Action.Builder(icon, "", pi).build()
    }

    private fun composeAlbumArtWithAppBadge(context: Context, albumBitmap: Bitmap, pkg: String): Bitmap =
        createBadgedAlbumArt(context, albumBitmap, pkg)

    /**
     * Bo góc cho ảnh bìa album (squircle bo cong mềm mại theo chuẩn thiết kế OriginOS 6)
     */
    private fun roundCorners(bitmap: Bitmap, cornerRadiusRatio: Float = 0.22f): Bitmap {
        return runCatching {
            val soft = ensureSoftwareBitmap(bitmap)
            val w = soft.width
            val h = soft.height
            if (w <= 0 || h <= 0) return bitmap
            val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
            val radius = minOf(w, h) * cornerRadiusRatio
            canvas.drawRoundRect(rect, radius, radius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(soft, 0f, 0f, paint)
            output
        }.getOrDefault(bitmap)
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }
    }

    private fun loadAppIconBitmap(context: Context, pkg: String, size: Int = 96): Bitmap? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(pkg)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        bmp
    }.getOrNull()

    /**
     * Tạo ảnh bìa mặc định màu tối sang trọng có vẽ icon ứng dụng phát nhạc ở giữa khi bài hát chưa có bìa album
     */
    private fun createDefaultAlbumBitmap(context: Context, pkg: String): Bitmap {
        val size = 192
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF242428")
        }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 38f, 38f, bgPaint)
        loadAppIconBitmap(context, pkg, 96)?.let { appBmp ->
            val left = (size - appBmp.width) / 2f
            val top = (size - appBmp.height) / 2f
            canvas.drawBitmap(appBmp, left, top, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        return output
    }

    /**
     * Vẽ bìa album vuông bo góc squircle kèm huy hiệu tròn (App Badge) của ứng dụng phát nhạc ở góc dưới bên phải
     */
    private fun createBadgedAlbumArt(context: Context, albumBitmap: Bitmap, pkg: String): Bitmap {
        return runCatching {
            val totalSize = 192
            val albumSize = 184
            val output = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            // 1. Vẽ albumBitmap đã bo góc squircle (bán kính 36f) vào vùng (0, 0, 184, 184)
            val squircleAlbum = Bitmap.createBitmap(albumSize, albumSize, Bitmap.Config.ARGB_8888)
            val albumCanvas = Canvas(squircleAlbum)
            val albumPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            albumCanvas.drawRoundRect(
                RectF(0f, 0f, albumSize.toFloat(), albumSize.toFloat()),
                36f,
                36f,
                albumPaint,
            )
            albumPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            val softAlbum = ensureSoftwareBitmap(albumBitmap)
            albumCanvas.drawBitmap(
                softAlbum,
                Rect(0, 0, softAlbum.width, softAlbum.height),
                Rect(0, 0, albumSize, albumSize),
                albumPaint,
            )
            canvas.drawBitmap(squircleAlbum, 0f, 0f, null)

            // 2. Lấy icon của ứng dụng phát nhạc và vẽ huy hiệu tròn ở góc dưới phải (tâm 152f, 152f)
            loadAppIconBitmap(context, pkg, 64)?.let { appBitmap ->
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FF181818")
                }
                canvas.drawCircle(152f, 152f, 34f, borderPaint)

                val badgeDiameter = 60
                val circularBadge = Bitmap.createBitmap(badgeDiameter, badgeDiameter, Bitmap.Config.ARGB_8888)
                val badgeCanvas = Canvas(circularBadge)
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                badgeCanvas.drawCircle(30f, 30f, 30f, badgePaint)
                badgePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                badgeCanvas.drawBitmap(
                    appBitmap,
                    Rect(0, 0, appBitmap.width, appBitmap.height),
                    Rect(0, 0, badgeDiameter, badgeDiameter),
                    badgePaint,
                )
                canvas.drawBitmap(circularBadge, 152f - 30f, 152f - 30f, null)
            }

            output
        }.getOrDefault(albumBitmap)
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
