package com.originisle.android.cards

import android.app.Notification
import android.app.NotificationManager
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
import com.originisle.android.R
import com.originisle.android.download.DownloadDetector
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache
import com.originisle.android.service.MediaControlReceiver
import com.originisle.android.service.MediaIslandLaunchActivity
import com.originisle.android.service.MediaIslandLaunchReceiver
import com.originisle.android.service.MediaIslandOverlay
import com.originisle.android.service.MediaSnapshot
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.ui.PREFS_NAME
import com.originisle.android.ui.PREF_MEDIA_OVERLAY_EXPAND
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

    @Volatile private var overlaySuppressed: Boolean = false
    @Volatile private var lastController: MediaController? = null
    @Volatile private var lastRawAlbumArt: Bitmap? = null
    @Volatile private var lastDurationMs: Long = 0L
    @Volatile private var lastPositionMs: Long = 0L
    @Volatile private var lastSourceActions: Array<Notification.Action>? = null
    @Volatile private var lastContentIntent: PendingIntent? = null

    private data class ArtCacheEntry(
        val key: String,
        val pillLeftIcon: Icon,
        val badgedAlbumIcon: Icon,
        val expandedArtBmp: Bitmap,
        val waveColors: ArrayList<String>,
    )

    @Volatile private var artCache: ArtCacheEntry? = null

    fun setOverlaySuppression(context: Context, suppressed: Boolean) {
        if (overlaySuppressed == suppressed) return
        overlaySuppressed = suppressed
        val appCtx = context.applicationContext
        if (suppressed) {
            // Cancel the SuperX notification synchronously so OriginOS removes the capsule
            // immediately without waiting for PlaygroundService's delayed endOrigin/cancel flow.
            val nm = appCtx.getSystemService(NotificationManager::class.java)
            nm?.cancel(OriginIslandConstants.SUPERX_TAG, ID)
            nm?.cancel(ID)
            val goneIntent = Intent(appCtx, PlaygroundService::class.java).apply {
                action = PlaygroundService.ACTION_GONE
                putExtra("id", ID)
            }
            appCtx.startService(goneIntent)
        } else if (lastPkg.isNotBlank() && activeMediaCastIds.contains(ID)) {
            dispatchMediaCard(
                context = appCtx,
                pkg = lastPkg,
                controller = lastController ?: IconCache.activeControllers[ID],
                track = lastTrack,
                artist = lastArtist,
                playing = lastPlaying == true,
                rawAlbumArt = lastRawAlbumArt,
                durationMs = lastDurationMs,
                positionMs = lastPositionMs,
                sourceActions = lastSourceActions,
                contentIntent = lastContentIntent,
                forceRefresh = true,
            )
        }
    }

    fun refreshPresentation(context: Context) {
        val appCtx = context.applicationContext
        if (!overlaySuppressed && lastPkg.isNotBlank() && activeMediaCastIds.contains(ID)) {
            dispatchMediaCard(
                context = appCtx,
                pkg = lastPkg,
                controller = lastController ?: IconCache.activeControllers[ID],
                track = lastTrack,
                artist = lastArtist,
                playing = lastPlaying == true,
                rawAlbumArt = lastRawAlbumArt,
                durationMs = lastDurationMs,
                positionMs = lastPositionMs,
                sourceActions = lastSourceActions,
                contentIntent = lastContentIntent,
                forceRefresh = true,
            )
        }
    }

    fun isCurrentlyPlaying(): Boolean = lastPlaying == true

    /** Các gói dịch vụ điều khiển đảo nhạc gốc trên OriginOS 6 (Vivo / iQOO) */
    val ORIGIN_NATIVE_MUSIC_PACKAGES = setOf(
        "com.vivo.musicwidgetmix",
        "com.vivo.mediacontrol",
        "com.android.bbkmusic",
        "com.vivo.music",
    )

    /** Các ứng dụng âm nhạc/audio nằm trong whitelist được Đảo gốc OriginOS Atomic Island hỗ trợ trực tiếp — nhường đảo gốc xử lý */
    val ORIGIN_SUPPORTED_MUSIC_PACKAGES = setOf(
        "com.apple.android.music",
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.tencent.qqmusic",
        "com.netease.cloudmusic",
        "com.heytap.music",
        "com.kugou.android",
        "com.kugou.android.lite",
        "com.kugou.viper",
        "cn.kuwo.player",
        "com.tencent.karaoke",
        "com.luna.music",
        "cmccwm.mobilemusic",
        "cn.missevan",
        "cn.wenyu.bodian",
        "com.duoduo.opera",
        "com.tencent.gqmusicpad",
        "com.aichang.yage",
        "com.blueocean.musicplayer",
        "org.videolan.vlc",
        "video.player",
        "com.luojilab.player",
        "com.tencent.wehear",
        "com.gidian.qdreader",
        "com.chaozh.ireaderfree",
        "com.vivo.globalanimation",
    )

    /** Các gói ứng dụng âm nhạc bên thứ ba phổ biến không bao giờ bị thu hồi nhầm */
    val THIRD_PARTY_MUSIC_PACKAGES = setOf(
        "com.spotify.music.lite",
        "com.spotify.lite",
        "com.spotify.music.canary",
        "com.zing.mp3",
        "com.vng.zingmp3",
        "vn.com.vng.zingmp3",
        "com.soundcloud.android",
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

            val isOngoing = active.isOngoing || (active.notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
            if (!isOngoing) continue

            // 1. Tiến trình widget đảo nhạc gốc của Vivo đang xuất hiện VÀ thực sự đang ongoing
            if (pkg in ORIGIN_NATIVE_MUSIC_PACKAGES) {
                return true
            }
            if (pkg in THIRD_PARTY_MUSIC_PACKAGES || NotificationCastListener.isKnownMusicApp(pkg)) continue
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

        if (DownloadDetector.isBrowserPackage(pkg)) {
            clear(context, ID)
            return
        }

        // 1. Nếu chính gói phát nhạc gốc của Vivo hoặc app nhạc đã được Đảo gốc hỗ trợ: nhường cho Đảo gốc, không can thiệp
        if (pkg in ORIGIN_NATIVE_MUSIC_PACKAGES || pkg in ORIGIN_SUPPORTED_MUSIC_PACKAGES) {
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
        val extraLargeBitmap = runCatching {
            n.extras?.getParcelable(NotificationCompat.EXTRA_LARGE_ICON, Bitmap::class.java)
        }.getOrNull()
        val notifLargeBitmap = extraLargeBitmap ?: notifLargeIcon?.loadDrawable(context)?.let { d ->
            val w = if (d.intrinsicWidth > 0) d.intrinsicWidth.coerceIn(48, 200) else 120
            val h = if (d.intrinsicHeight > 0) d.intrinsicHeight.coerceIn(48, 200) else 120
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            d.setBounds(0, 0, w, h)
            d.draw(cv)
            bmp
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
        if (DownloadDetector.isBrowserPackage(pkgLower) ||
            pkgLower in ORIGIN_NATIVE_MUSIC_PACKAGES ||
            pkgLower in ORIGIN_SUPPORTED_MUSIC_PACKAGES
        ) {
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
        if (stateUnchanged && !forceRefresh && PlaygroundService.activeCardIds.contains(ID)) {
            lastController = controller
            lastDurationMs = durationMs
            lastPositionMs = positionMs
            lastSourceActions = sourceActions
            lastContentIntent = contentIntent
            IconCache.activeLargeBitmaps[ID]?.let { cachedArt ->
                val clickIntent = contentIntent ?: fallbackClickIntent(context, pkg)
                MediaIslandOverlay.updateSnapshot(
                    MediaSnapshot(
                        pkg = pkg,
                        track = track,
                        artist = artist,
                        playing = playing,
                        badgedAlbumArt = cachedArt,
                        durationMs = durationMs,
                        positionMs = positionMs,
                        controller = controller,
                        contentIntent = clickIntent,
                    ),
                )
            }
            return
        }
        lastTrack = track
        lastArtist = artist
        lastPlaying = playing
        lastPkg = pkg
        lastUiNightMode = isNightMode
        lastArtSignature = artSignature
        lastDispatchTimeMs = now
        lastController = controller
        lastRawAlbumArt = rawAlbumArt
        lastDurationMs = durationMs
        lastPositionMs = positionMs
        lastSourceActions = sourceActions
        lastContentIntent = contentIntent

        val artCacheKey = "$pkg|$artSignature"
        val artEntry = artCache?.takeIf { it.key == artCacheKey } ?: run {
            val baseAlbumBitmap: Bitmap = rawAlbumArt ?: createDefaultAlbumBitmap(context, pkg)
            val artBmp: Bitmap = roundCorners(baseAlbumBitmap, 0.20f)
            val expandedArtBmp: Bitmap = composeAlbumArtWithAppBadge(context, artBmp, pkg)
            val pillLeftIcon: Icon = Icon.createWithBitmap(artBmp)
            val badgedAlbumIcon: Icon = Icon.createWithBitmap(expandedArtBmp)
            val waveColors = ArrayList(waveColorsFrom(rawAlbumArt ?: baseAlbumBitmap))
            ArtCacheEntry(
                key = artCacheKey,
                pillLeftIcon = pillLeftIcon,
                badgedAlbumIcon = badgedAlbumIcon,
                expandedArtBmp = expandedArtBmp,
                waveColors = waveColors,
            ).also { artCache = it }
        }

        val expandedArtBmp = artEntry.expandedArtBmp
        val pillLeftIcon = artEntry.pillLeftIcon
        val badgedAlbumIcon = artEntry.badgedAlbumIcon
        val waveColors = artEntry.waveColors

        IconCache.activeSmallIcons[ID] = pillLeftIcon
        IconCache.activeLargeBitmaps[ID] = expandedArtBmp

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

        val prevSourceAction = findSourceAction(listOf("previous", "skip back", "rewind", "trước", "lùi"))
        val nextSourceAction = findSourceAction(listOf("next", "skip forward", "fast forward", "tiếp", "tới"))
        val sessionActions = controller?.playbackState?.actions ?: 0L
        val hasPrev = prevSourceAction != null || (sessionActions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L
        val hasNext = nextSourceAction != null || (sessionActions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L

        val actions = arrayListOf<Notification.Action>()
        if (hasPrev) {
            actions.add(
                makeAction(context, R.drawable.ic_media_prev, prevSourceAction ?: mediaAction(MediaControlReceiver.ACTION_PREV)),
            )
        }
        actions.add(
            makeAction(
                context,
                if (playing) R.drawable.ic_media_pause else R.drawable.ic_media_play,
                mediaAction(MediaControlReceiver.ACTION_PLAY_PAUSE),
            ),
        )
        if (hasNext) {
            actions.add(
                makeAction(context, R.drawable.ic_media_next, nextSourceAction ?: mediaAction(MediaControlReceiver.ACTION_NEXT)),
            )
        }

        val contentClickIntent: PendingIntent = contentIntent ?: fallbackClickIntent(context, pkg)

        val snapshot = MediaSnapshot(
            pkg = pkg,
            track = track,
            artist = artist,
            playing = playing,
            badgedAlbumArt = expandedArtBmp,
            durationMs = durationMs,
            positionMs = positionMs,
            controller = controller,
            contentIntent = contentClickIntent,
        )
        MediaIslandOverlay.updateSnapshot(snapshot)

        pendingDismissTasks.remove(ID)?.let { handler.removeCallbacks(it) }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val useHybridOverlay = MediaIslandOverlay.canShow(context) &&
            prefs.getBoolean(PREF_MEDIA_OVERLAY_EXPAND, true)
        val overlayBroadcastPi = PendingIntent.getBroadcast(
            context,
            ID + 99,
            Intent(context, MediaIslandLaunchReceiver::class.java).apply {
                action = MediaIslandLaunchReceiver.ACTION_LAUNCH_MEDIA_OVERLAY
                putExtra(MediaIslandLaunchActivity.EXTRA_FALLBACK_INTENT, contentClickIntent)
                putExtra(MediaIslandLaunchActivity.EXTRA_SOURCE_PKG, pkg)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val chip = if (playing) "Playing" else " "
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("is_ongoing", true)

            putExtra("oi_scene", "NAVIGATION")
            putExtra("title", track)
            putExtra("text", artist)
            putExtra("source_pkg", pkg)
            putExtra("oi_left_content", if (rawAlbumArt != null) "" else track)
            putExtra("status_chip_text", if (playing) " " else "  ")

            putExtra("oi_left_icon", pillLeftIcon)
            putExtra("oi_large_icon", badgedAlbumIcon)
            putExtra("oi_show_left_icon", true)
            putExtra("icon_res", R.drawable.ic_media_play)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
            putExtra("oi_btn_transparent", true)
            putExtra("oi_bg_color", "#FF262626")
            putExtra("oi_fg_color", "#FFFFFFFF")
            putExtra("oi_island_click_expand", !useHybridOverlay)
            putExtra("oi_show_notify", false)

            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
            putExtra("oi_wave_state", if (playing) 1 else 0)
            if (waveColors.isNotEmpty()) {
                putStringArrayListExtra("oi_wave_color_list", waveColors)
            }
            putExtra(
                "oi_right_icon",
                if (playing) createAnimatedWaveIcon(context, waveColors)
                else createPausedWaveIcon(waveColors),
            )
            putExtra("oi_show_right_icon", true)
            putExtra("oi_right_content", if (playing) "" else " ")

            putParcelableArrayListExtra("actions", actions)
            putExtra(
                "source_content_intent",
                if (useHybridOverlay) overlayBroadcastPi else contentClickIntent,
            )
        }
        if (!overlaySuppressed) {
            context.startService(intent)
            CastLog.add("Nhạc", track, "hiển thị — $track ($artist)", true)
        }
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
            MediaIslandOverlay.dismiss()
            lastTrack = ""
            lastArtist = ""
            lastPlaying = null
            lastPkg = ""
            lastUiNightMode = null
            lastArtSignature = ""
            lastDispatchTimeMs = 0L
            overlaySuppressed = false
            lastController = null
            lastRawAlbumArt = null
            lastDurationMs = 0L
            lastPositionMs = 0L
            lastSourceActions = null
            lastContentIntent = null
            artCache = null
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

    private fun makeAction(context: Context, iconRes: Int, pi: PendingIntent): Notification.Action {
        val icon = Icon.createWithResource(context, iconRes)
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

    private fun primaryWaveColor(waveColors: List<String>): Int {
        val hex = waveColors.firstOrNull() ?: "#68A4FF"
        return runCatching { Color.parseColor(hex) }.getOrElse { Color.parseColor("#68A4FF") }
    }

    private fun createAnimatedWaveIcon(context: Context, waveColors: List<String>): Icon {
        return Icon.createWithResource(context, R.drawable.avd_music_wave).apply {
            setTint(primaryWaveColor(waveColors))
        }
    }

    /**
     * Vẽ 5 chấm tròn nhỏ đồng bộ vị trí và màu chính nhạt dần (trạng thái sóng dừng lại khi tạm dừng phát)
     */
    private fun createPausedWaveIcon(waveColors: List<String>, size: Int = 96): Icon {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val baseColor = primaryWaveColor(waveColors)
        val alphas = intArrayOf(210, 180, 150, 118, 86)
        val padX = size * 0.05f
        val usableW = size - padX * 2
        val gap = usableW * 0.085f
        val dotSize = (usableW - gap * 4) / 5f
        val cy = size / 2f
        val r = dotSize / 2f
        for (i in 0 until 5) {
            paint.color = Color.argb(
                alphas[i],
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor),
            )
            val cx = padX + i * (dotSize + gap) + r
            canvas.drawCircle(cx, cy, r, paint)
        }
        return Icon.createWithBitmap(bmp)
    }

    /**
     * Trích xuất màu chủ đạo thực tế từ toàn bộ ảnh bìa album (lưới 16x16 theo Hue bucket)
     */
    private fun waveColorsFrom(bitmap: Bitmap?): List<String> {
        val fallback = listOf("#68A4FF", "#8FB6D9", "#B89CC7")
        if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) return fallback
        return runCatching {
            val src = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return fallback
            } else {
                bitmap
            }
            val thumb = Bitmap.createScaledBitmap(src, 16, 16, true)
            try {
                val bucketCount = 24
                val weights = FloatArray(bucketCount)
                val sumR = FloatArray(bucketCount)
                val sumG = FloatArray(bucketCount)
                val sumB = FloatArray(bucketCount)
                val hsv = FloatArray(3)
                for (y in 0 until thumb.height) {
                    for (x in 0 until thumb.width) {
                        val pixel = thumb.getPixel(x, y)
                        if (Color.alpha(pixel) < 128) continue
                        Color.colorToHSV(pixel, hsv)
                        // Bỏ qua vùng quá tối hoặc quá xám trắng trừ khi không có màu nào khác
                        val sat = hsv[1]
                        val valB = hsv[2]
                        if (valB < 0.14f) continue
                        val weight = (0.25f + sat * 1.5f) * (0.4f + valB * 0.6f)
                        val bucket = ((hsv[0] / 360f) * bucketCount).toInt().coerceIn(0, bucketCount - 1)
                        weights[bucket] += weight
                        sumR[bucket] += Color.red(pixel) * weight
                        sumG[bucket] += Color.green(pixel) * weight
                        sumB[bucket] += Color.blue(pixel) * weight
                    }
                }
                val ranked = (0 until bucketCount)
                    .filter { weights[it] > 0f }
                    .sortedByDescending { weights[it] }
                if (ranked.isEmpty()) return fallback
                val result = ranked.take(3).map { b ->
                    val w = weights[b]
                    val r = (sumR[b] / w).toInt().coerceIn(0, 255)
                    val g = (sumG[b] / w).toInt().coerceIn(0, 255)
                    val bl = (sumB[b] / w).toInt().coerceIn(0, 255)
                    Color.RGBToHSV(r, g, bl, hsv)
                    // Giữ đúng tông màu bìa nhưng đảm bảo đủ sáng để nổi rõ trên nền pill tối
                    if (hsv[2] < 0.72f) hsv[2] = 0.85f
                    if (hsv[1] in 0.08f..0.35f) hsv[1] = 0.45f
                    val adjusted = Color.HSVToColor(hsv)
                    String.format("#%06X", 0xFFFFFF and adjusted)
                }
                if (result.isNotEmpty()) result else fallback
            } finally {
                if (thumb != src) thumb.recycle()
                if (src != bitmap) src.recycle()
            }
        }.getOrDefault(fallback)
    }
}
