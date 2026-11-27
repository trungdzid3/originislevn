package com.originisle.android.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.originisle.android.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class MediaSnapshot(
    val pkg: String,
    val track: String,
    val artist: String,
    val playing: Boolean,
    val badgedAlbumArt: Bitmap?,
    val durationMs: Long,
    val positionMs: Long,
    val controller: MediaController?,
    val contentIntent: PendingIntent?,
)

/**
 * Floating OriginOS 6 style Music Island overlay rendered via [WindowManager]
 * with live playback progress interpolation, seekbar scrubbing, and output picker shortcut.
 */
object MediaIslandOverlay {

    private const val TAG = "MediaIslandOverlay"
    private const val AUTO_HIDE_MS = 6_000L
    private const val PROGRESS_TICK_MS = 1_000L
    private const val ANIM_DURATION_MS = 200L

    private const val CARD_BG_COLOR = 0xF0101014.toInt()
    private const val CARD_EDGE_LIGHT_TOP = 0x42FFFFFF
    private const val CARD_EDGE_LIGHT_BOTTOM = 0x0FFFFFFF
    private const val OUTPUT_BTN_BG_COLOR = 0x1FFFFFFF
    private const val TEXT_PRIMARY = Color.WHITE
    private const val TEXT_SECONDARY = 0xB3FFFFFF.toInt()
    private const val SEEK_BG_COLOR = 0x3DFFFFFF
    private const val RIPPLE_COLOR = 0x33FFFFFF

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var latestSnapshot: MediaSnapshot? = null

    @Volatile
    private var rootView: FrameLayout? = null

    @Volatile
    private var cardView: LinearLayout? = null

    @Volatile
    private var windowManager: WindowManager? = null

    @Volatile
    private var isDismissing = false

    private var albumArtView: ImageView? = null
    private var trackView: TextView? = null
    private var artistView: TextView? = null
    private var currentTimeView: TextView? = null
    private var totalTimeView: TextView? = null
    private var seekBarView: SeekBar? = null
    private var playPauseButton: ImageButton? = null

    private var isUserSeeking = false

    private val autoHideRunnable = Runnable { dismiss() }

    private val progressRunnable = object : Runnable {
        override fun run() {
            val snapshot = latestSnapshot
            if (rootView == null || isDismissing || snapshot == null || !snapshot.playing) {
                return
            }
            if (!isUserSeeking) {
                val pos = computeInterpolatedPosition(snapshot)
                bindProgress(pos, snapshot.durationMs)
            }
            mainHandler.postDelayed(this, PROGRESS_TICK_MS)
        }
    }

    fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun updateSnapshot(snapshot: MediaSnapshot) {
        latestSnapshot = snapshot
        if (rootView == null || isDismissing) return

        if (Looper.myLooper() == Looper.getMainLooper()) {
            bindSnapshotOnMainThread(snapshot)
        } else {
            mainHandler.post {
                if (rootView != null && !isDismissing) {
                    bindSnapshotOnMainThread(snapshot)
                }
            }
        }
    }

    fun show(context: Context, snapshot: MediaSnapshot? = null): Boolean {
        if (!canShow(context)) return false
        if (snapshot != null) {
            latestSnapshot = snapshot
        }
        val targetSnapshot = latestSnapshot ?: return false
        val appContext = context.applicationContext

        if (Looper.myLooper() == Looper.getMainLooper()) {
            return showOnMainThread(appContext, targetSnapshot)
        }
        mainHandler.post { showOnMainThread(appContext, targetSnapshot) }
        return true
    }

    fun toggle(context: Context) {
        if (rootView != null && !isDismissing) {
            dismiss()
        } else {
            show(context, null)
        }
    }

    fun dismiss() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dismissOnMainThread()
        } else {
            mainHandler.post { dismissOnMainThread() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOnMainThread(appContext: Context, snapshot: MediaSnapshot): Boolean {
        if (rootView != null && !isDismissing) {
            bindSnapshotOnMainThread(snapshot)
            resetAutoHideTimer()
            return true
        }
        removeWindowImmediate()

        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return false
        val density = appContext.resources.displayMetrics.density
        fun dp(value: Float): Int = (value * density).roundToInt()

        val metrics = wm.currentWindowMetrics
        val screenWidth = metrics.bounds.width()
        val overlayWidth = (screenWidth * 0.92f).roundToInt().coerceAtMost(dp(420f))
        val statusBarTop = metrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.statusBars())
            .top

        val root = object : FrameLayout(appContext) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    dismiss()
                    return true
                }
                return super.onTouchEvent(event)
            }
        }

        val touchSlop = ViewConfiguration.get(appContext).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var isDraggingUp = false

        val card = object : LinearLayout(appContext) {
            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = ev.rawX
                        downY = ev.rawY
                        isDraggingUp = false
                        resetAutoHideTimer()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = ev.rawX - downX
                        val dy = ev.rawY - downY
                        if (dy < -touchSlop && abs(dy) > abs(dx)) {
                            isDraggingUp = true
                            return true
                        }
                    }
                }
                return super.onInterceptTouchEvent(ev)
            }
        }.apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(14f), dp(16f), dp(12f))
            val strokePx = (1f * density).roundToInt().coerceAtLeast(1)
            val outerCorner = dp(30f).toFloat()
            val innerCorner = (outerCorner - strokePx).coerceAtLeast(0f)
            val edgeLightBorder = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(CARD_EDGE_LIGHT_TOP, CARD_EDGE_LIGHT_BOTTOM),
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = outerCorner
            }
            val innerSurface = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = innerCorner
                setColor(CARD_BG_COLOR)
            }
            background = LayerDrawable(arrayOf(edgeLightBorder, innerSurface)).apply {
                setLayerInset(1, strokePx, strokePx, strokePx, strokePx)
            }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }

        card.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    isDraggingUp = false
                    resetAutoHideTimer()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - downY
                    if (!isDraggingUp && dy < -touchSlop) {
                        isDraggingUp = true
                    }
                    if (isDraggingUp) {
                        v.translationY = dy.coerceAtMost(0f)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDraggingUp) {
                        if (v.translationY < -dp(40f)) {
                            dismiss()
                        } else {
                            v.animate()
                                .translationY(0f)
                                .setDuration(150L)
                                .setInterpolator(DecelerateInterpolator())
                                .start()
                        }
                        isDraggingUp = false
                    }
                    true
                }
                else -> false
            }
        }

        // Row 1: Album Art + Track/Artist + Audio Output Button
        val topRow = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val albumArt = ImageView(appContext).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14f).toFloat()
                setColor(0x22FFFFFF)
            }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(dp(52f), dp(52f))
            setOnClickListener {
                openSourceApp(appContext)
                dismiss()
            }
        }

        val textColumn = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12f)
                marginEnd = dp(10f)
            }
        }

        val trackTv = TextView(appContext).apply {
            setTextColor(TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            isSingleLine = true
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isSelected = true
        }

        val artistTv = TextView(appContext).apply {
            setTextColor(TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            isSingleLine = true
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(2f)
            }
        }

        textColumn.addView(trackTv)
        textColumn.addView(artistTv)

        val outputBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(OUTPUT_BTN_BG_COLOR)
        }
        val outputRipple = RippleDrawable(
            ColorStateList.valueOf(RIPPLE_COLOR),
            outputBg,
            outputBg,
        )
        val outputBtn = ImageButton(appContext).apply {
            setImageResource(R.drawable.ic_media_output)
            imageTintList = ColorStateList.valueOf(TEXT_PRIMARY)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val iconPad = dp(8f)
            setPadding(iconPad, iconPad, iconPad, iconPad)
            background = outputRipple
            contentDescription = "Audio output"
            layoutParams = LinearLayout.LayoutParams(dp(36f), dp(36f))
            setOnClickListener {
                openAudioOutputPicker(appContext)
                dismiss()
            }
        }

        topRow.addView(albumArt)
        topRow.addView(textColumn)
        topRow.addView(outputBtn)
        card.addView(topRow)

        // Row 2: Current Time + SeekBar + Total Time
        val seekRow = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(12f)
            }
        }

        val curTimeTv = TextView(appContext).apply {
            setTextColor(TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            fontFeatureSettings = "tnum"
            text = "00:00"
        }

        val seekBar = SeekBar(appContext).apply {
            max = 1000
            splitTrack = false
            progressTintList = ColorStateList.valueOf(TEXT_PRIMARY)
            progressBackgroundTintList = ColorStateList.valueOf(SEEK_BG_COLOR)
            thumb = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(TEXT_PRIMARY)
                setSize(dp(10f), dp(10f))
            }
            thumbOffset = dp(5f)
            setPadding(dp(8f), dp(6f), dp(8f), dp(6f))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(4f)
                marginEnd = dp(4f)
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val duration = latestSnapshot?.durationMs ?: 0L
                    if (duration > 0L) {
                        val previewPos = (duration * progress) / 1000L
                        currentTimeView?.text = formatDuration(previewPos)
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {
                    isUserSeeking = true
                    resetAutoHideTimer()
                }

                override fun onStopTrackingTouch(sb: SeekBar?) {
                    isUserSeeking = false
                    resetAutoHideTimer()
                    val current = latestSnapshot ?: return
                    val duration = current.durationMs
                    if (duration > 0L && sb != null) {
                        val targetMs = (duration * sb.progress) / 1000L
                        current.controller?.transportControls?.seekTo(targetMs)
                        bindProgress(targetMs, duration)
                    }
                }
            })
        }

        val totalTimeTv = TextView(appContext).apply {
            setTextColor(TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            fontFeatureSettings = "tnum"
            text = "00:00"
        }

        seekRow.addView(curTimeTv)
        seekRow.addView(seekBar)
        seekRow.addView(totalTimeTv)
        card.addView(seekRow)

        // Row 3: Prev / Play-Pause / Next (naked borderless icons)
        val controlsRow = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(8f)
            }
        }

        fun createControlButton(resId: Int, desc: String, onClick: () -> Unit): ImageButton {
            val ripple = RippleDrawable(
                ColorStateList.valueOf(RIPPLE_COLOR),
                null,
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.WHITE)
                },
            )
            return ImageButton(appContext).apply {
                setImageResource(resId)
                imageTintList = ColorStateList.valueOf(TEXT_PRIMARY)
                scaleType = ImageView.ScaleType.FIT_CENTER
                val pad = dp(8f)
                setPadding(pad, pad, pad, pad)
                background = ripple
                contentDescription = desc
                val size = dp(42f) // 26dp icon + 2 * 8dp padding
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = dp(18f)
                    marginEnd = dp(18f)
                }
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            resetAutoHideTimer()
                            v.animate().cancel()
                            v.animate()
                                .scaleX(0.90f)
                                .scaleY(0.90f)
                                .alpha(0.85f)
                                .setDuration(90L)
                                .setInterpolator(DecelerateInterpolator())
                                .start()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate().cancel()
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .alpha(1f)
                                .setDuration(180L)
                                .setInterpolator(OvershootInterpolator(1.6f))
                                .start()
                        }
                    }
                    false
                }
                setOnClickListener {
                    resetAutoHideTimer()
                    onClick()
                }
            }
        }

        val prevBtn = createControlButton(R.drawable.ic_media_prev, "Previous") {
            latestSnapshot?.controller?.transportControls?.skipToPrevious()
        }
        val playPauseBtn = createControlButton(R.drawable.ic_media_play, "Play or pause") {
            val current = latestSnapshot ?: return@createControlButton
            val controls = current.controller?.transportControls ?: return@createControlButton
            val state = current.controller.playbackState?.state
            val isCurrentlyPlaying = state == PlaybackState.STATE_PLAYING || (state == null && current.playing)
            if (isCurrentlyPlaying) {
                controls.pause()
            } else {
                controls.play()
            }
        }
        val nextBtn = createControlButton(R.drawable.ic_media_next, "Next") {
            latestSnapshot?.controller?.transportControls?.skipToNext()
        }

        controlsRow.addView(prevBtn)
        controlsRow.addView(playPauseBtn)
        controlsRow.addView(nextBtn)
        card.addView(controlsRow)

        root.addView(
            card,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        val windowParams = WindowManager.LayoutParams(
            overlayWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarTop + dp(8f)
        }

        return runCatching {
            wm.addView(root, windowParams)
            rootView = root
            cardView = card
            windowManager = wm
            isDismissing = false

            albumArtView = albumArt
            trackView = trackTv
            artistView = artistTv
            currentTimeView = curTimeTv
            totalTimeView = totalTimeTv
            seekBarView = seekBar
            playPauseButton = playPauseBtn

            bindSnapshotOnMainThread(snapshot)

            card.alpha = 0f
            card.scaleX = 0.92f
            card.scaleY = 0.92f
            card.translationY = -dp(10f).toFloat()
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(ANIM_DURATION_MS)
                .setInterpolator(DecelerateInterpolator())
                .start()

            resetAutoHideTimer()
            true
        }.onFailure {
            Log.e(TAG, "Failed to show MediaIslandOverlay", it)
            removeWindowImmediate()
        }.getOrDefault(false)
    }

    private fun bindSnapshotOnMainThread(snapshot: MediaSnapshot) {
        val ctx = rootView?.context
        trackView?.text = snapshot.track.ifBlank { "Unknown track" }
        artistView?.text = snapshot.artist

        val artView = albumArtView
        if (artView != null) {
            if (snapshot.badgedAlbumArt != null) {
                artView.setImageBitmap(snapshot.badgedAlbumArt)
            } else if (ctx != null) {
                val appIcon = runCatching {
                    ctx.packageManager.getApplicationIcon(snapshot.pkg)
                }.getOrNull()
                if (appIcon != null) {
                    artView.setImageDrawable(appIcon)
                } else {
                    artView.setImageResource(R.drawable.ic_media_placeholder)
                }
            }
        }

        playPauseButton?.setImageResource(
            if (snapshot.playing) R.drawable.ic_media_pause else R.drawable.ic_media_play,
        )

        if (!isUserSeeking) {
            val pos = computeInterpolatedPosition(snapshot)
            bindProgress(pos, snapshot.durationMs)
        }

        mainHandler.removeCallbacks(progressRunnable)
        if (snapshot.playing && rootView != null && !isDismissing) {
            mainHandler.postDelayed(progressRunnable, PROGRESS_TICK_MS)
        }
    }

    private fun computeInterpolatedPosition(snapshot: MediaSnapshot): Long {
        val state = snapshot.controller?.playbackState
        val duration = snapshot.durationMs.coerceAtLeast(0L)
        if (state == null) {
            return snapshot.positionMs.coerceIn(0L, duration.coerceAtLeast(snapshot.positionMs))
        }
        val basePos = state.position.coerceAtLeast(0L)
        val pos = if (state.state == PlaybackState.STATE_PLAYING && state.lastPositionUpdateTime > 0L) {
            val elapsed = (SystemClock.elapsedRealtime() - state.lastPositionUpdateTime).coerceAtLeast(0L)
            val speed = if (state.playbackSpeed > 0f) state.playbackSpeed else 1f
            basePos + (elapsed * speed).toLong()
        } else {
            basePos
        }
        return if (duration > 0L) pos.coerceIn(0L, duration) else pos
    }

    private fun bindProgress(positionMs: Long, durationMs: Long) {
        val safeDuration = durationMs.coerceAtLeast(0L)
        val safePos = if (safeDuration > 0L) positionMs.coerceIn(0L, safeDuration) else positionMs.coerceAtLeast(0L)
        currentTimeView?.text = formatDuration(safePos)
        totalTimeView?.text = formatDuration(safeDuration)
        val sb = seekBarView ?: return
        sb.isEnabled = safeDuration > 0L
        sb.progress = if (safeDuration > 0L) {
            ((safePos * 1000L) / safeDuration).toInt().coerceIn(0, 1000)
        } else {
            0
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L)) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private fun openSourceApp(appContext: Context) {
        val snapshot = latestSnapshot ?: return
        val pending = snapshot.contentIntent
        if (pending != null) {
            val sent = runCatching {
                pending.send()
                true
            }.getOrDefault(false)
            if (sent) return
        }
        runCatching {
            appContext.packageManager.getLaunchIntentForPackage(snapshot.pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }?.let { appContext.startActivity(it) }
        }.onFailure {
            Log.w(TAG, "Failed to launch ${snapshot.pkg}", it)
        }
    }

    private fun openAudioOutputPicker(appContext: Context) {
        val panelIntent = Intent("com.android.settings.panel.action.VOLUME").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(panelIntent)
        } catch (_: ActivityNotFoundException) {
            runCatching {
                appContext.startActivity(
                    Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }.onFailure {
                Log.w(TAG, "Failed to open sound settings", it)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to open volume panel", t)
        }
    }

    private fun resetAutoHideTimer() {
        mainHandler.removeCallbacks(autoHideRunnable)
        if (rootView != null && !isDismissing) {
            mainHandler.postDelayed(autoHideRunnable, AUTO_HIDE_MS)
        }
    }

    private fun dismissOnMainThread() {
        val root = rootView ?: return
        if (isDismissing) return
        isDismissing = true

        mainHandler.removeCallbacks(autoHideRunnable)
        mainHandler.removeCallbacks(progressRunnable)

        val card = cardView
        if (card == null) {
            removeWindowImmediate()
            return
        }

        val density = root.resources.displayMetrics.density
        card.animate().cancel()
        card.animate()
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .translationY(-10f * density)
            .setDuration(ANIM_DURATION_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { removeWindowImmediate() }
            .start()
    }

    private fun removeWindowImmediate() {
        mainHandler.removeCallbacks(autoHideRunnable)
        mainHandler.removeCallbacks(progressRunnable)

        val root = rootView
        val wm = windowManager
        rootView = null
        cardView = null
        windowManager = null
        albumArtView = null
        trackView = null
        artistView = null
        currentTimeView = null
        totalTimeView = null
        seekBarView = null
        playPauseButton = null
        isUserSeeking = false
        isDismissing = false

        if (root != null) {
            if (wm != null) {
                try {
                    wm.removeViewImmediate(root)
                } catch (t: Throwable) {
                    Log.w(TAG, "removeViewImmediate failed", t)
                }
            }
            root.removeAllViews()
        }
    }
}
