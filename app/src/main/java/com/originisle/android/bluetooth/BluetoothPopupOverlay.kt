package com.originisle.android.bluetooth

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

data class BluetoothPopupBattery(
    val main: Int = -1,
    val left: Int = -1,
    val right: Int = -1,
    val caseBattery: Int = -1,
)

object BluetoothPopupOverlay {

    private const val COOLDOWN_MS = 12_000L
    private const val AUTO_DISMISS_MS = 4_500L
    private const val ENTER_ANIM_MS = 260L
    private const val EXIT_ANIM_MS = 180L

    private const val COLOR_ACCENT_GREEN = 0xFF00D084.toInt()
    private const val COLOR_LOW_RED = 0xFFFF5247.toInt()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lastShownElapsedMs = ConcurrentHashMap<String, Long>()

    private var currentView: View? = null
    private var currentWindowManager: WindowManager? = null
    private var currentKey: String? = null
    private var currentAnimatedDrawable: AnimatedImageDrawable? = null
    private var currentBatteryContainer: LinearLayout? = null
    private var currentIsDark: Boolean = true
    private var isDismissing: Boolean = false

    private val autoDismissRunnable = Runnable {
        dismissOnMainThread()
    }

    fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun showOrUpdate(
        context: Context,
        addressOrKey: String,
        deviceName: String,
        battery: BluetoothPopupBattery,
        forcePreview: Boolean = false,
    ): Boolean {
        if (!canShow(context)) return false
        val appContext = context.applicationContext
        val key = BluetoothPopupStore.normalizeKey(addressOrKey)
        val config = BluetoothPopupStore.getDeviceConfig(appContext, addressOrKey, deviceName)

        if (!forcePreview) {
            if (!BluetoothPopupStore.isPopupGloballyEnabled(appContext)) return false
            if (!config.enabled) return false

            val isAlreadyShowingSame = currentKey == key && currentView != null && !isDismissing
            if (!isAlreadyShowingSame) {
                val lastShown = lastShownElapsedMs[key] ?: 0L
                if (SystemClock.elapsedRealtime() - lastShown < COOLDOWN_MS) {
                    return false
                }
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            showOrUpdateOnMainThread(appContext, key, deviceName, config, battery, forcePreview)
        } else {
            mainHandler.post {
                showOrUpdateOnMainThread(appContext, key, deviceName, config, battery, forcePreview)
            }
        }
        return true
    }

    fun updateBatteryIfShowing(addressOrKey: String, battery: BluetoothPopupBattery) {
        val key = BluetoothPopupStore.normalizeKey(addressOrKey)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateBatteryOnMainThread(key, battery)
        } else {
            mainHandler.post { updateBatteryOnMainThread(key, battery) }
        }
    }

    fun onDeviceDisconnected(addressOrKey: String) {
        val key = BluetoothPopupStore.normalizeKey(addressOrKey)
        lastShownElapsedMs.remove(key)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (currentKey == key) {
                dismissOnMainThread()
            }
        } else {
            mainHandler.post {
                if (currentKey == key) {
                    dismissOnMainThread()
                }
            }
        }
    }

    fun dismiss() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dismissOnMainThread()
        } else {
            mainHandler.post { dismissOnMainThread() }
        }
    }

    private fun showOrUpdateOnMainThread(
        context: Context,
        key: String,
        deviceName: String,
        config: BluetoothPopupDeviceConfig,
        battery: BluetoothPopupBattery,
        forcePreview: Boolean,
    ) {
        val existingContainer = currentBatteryContainer
        if (currentKey == key && currentView != null && existingContainer != null && !isDismissing) {
            renderBatterySection(context, existingContainer, battery, currentIsDark)
            scheduleAutoDismiss()
            return
        }

        removeImmediate()

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val screenWidth = wm.currentWindowMetrics.bounds.width()
        val cardWidth = min(screenWidth - dp(context, 32), dp(context, 380))

        val card = buildCardView(context, config, deviceName, battery, isDark)

        val params = WindowManager.LayoutParams(
            cardWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(context, 24)
        }

        card.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismissOnMainThread()
                true
            } else {
                false
            }
        }

        try {
            wm.addView(card, params)
        } catch (_: Exception) {
            cleanupReferences()
            return
        }

        currentView = card
        currentWindowManager = wm
        currentKey = key
        currentIsDark = isDark
        isDismissing = false

        if (!forcePreview) {
            lastShownElapsedMs[key] = SystemClock.elapsedRealtime()
        }

        card.translationY = dp(context, 80).toFloat()
        card.alpha = 0f
        card.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(ENTER_ANIM_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        scheduleAutoDismiss()
    }

    private fun updateBatteryOnMainThread(key: String, battery: BluetoothPopupBattery) {
        val view = currentView ?: return
        val container = currentBatteryContainer ?: return
        if (currentKey != key || isDismissing) return
        renderBatterySection(view.context, container, battery, currentIsDark)
    }

    private fun dismissOnMainThread() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        val view = currentView ?: return
        if (isDismissing) return
        isDismissing = true

        view.animate().cancel()
        view.animate()
            .translationY(dp(view.context, 80).toFloat())
            .alpha(0f)
            .setDuration(EXIT_ANIM_MS)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    removeImmediate()
                }

                override fun onAnimationCancel(animation: Animator) {
                    removeImmediate()
                }
            })
            .start()
    }

    private fun removeImmediate() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        val view = currentView
        val wm = currentWindowManager
        if (view != null) {
            view.animate().cancel()
            view.setOnTouchListener(null)
            if (wm != null) {
                try {
                    wm.removeViewImmediate(view)
                } catch (_: Exception) {
                }
            }
        }
        cleanupReferences()
    }

    private fun cleanupReferences() {
        currentAnimatedDrawable?.stop()
        currentAnimatedDrawable = null
        currentView = null
        currentWindowManager = null
        currentKey = null
        currentBatteryContainer = null
        isDismissing = false
    }

    private fun scheduleAutoDismiss() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        mainHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_MS)
    }

    private fun buildCardView(
        context: Context,
        config: BluetoothPopupDeviceConfig,
        deviceName: String,
        battery: BluetoothPopupBattery,
        isDark: Boolean,
    ): LinearLayout {
        val bgColor = if (isDark) 0xF51B1D22.toInt() else 0xF8FFFFFF.toInt()
        val strokeColor = if (isDark) 0x29FFFFFF else 0x1A000000
        val primaryText = if (isDark) 0xFFF4F6FA.toInt() else 0xFF14161A.toInt()
        val closeBgColor = if (isDark) 0x1FFFFFFF else 0x12000000
        val closeTextColor = if (isDark) 0xCCFFFFFF.toInt() else 0xCC14161A.toInt()

        val cardBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 28).toFloat()
            setColor(bgColor)
            setStroke(dp(context, 1), strokeColor)
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg
            elevation = dp(context, 16).toFloat()
            setPadding(dp(context, 20), dp(context, 18), dp(context, 20), dp(context, 20))
        }

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val titleStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(context, 12)
            }
        }

        val resolvedName = config.customName?.takeIf { it.isNotBlank() }
            ?: deviceName.ifBlank { config.preset.label }

        val titleView = TextView(context).apply {
            text = resolvedName
            setTextColor(primaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        val subtitleView = TextView(context).apply {
            text = "Đã kết nối"
            setTextColor(COLOR_ACCENT_GREEN)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, dp(context, 2), 0, 0)
        }

        titleStack.addView(titleView)
        titleStack.addView(subtitleView)

        val closeBtnBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(closeBgColor)
        }
        val closeBtn = TextView(context).apply {
            text = "\u2715"
            setTextColor(closeTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            background = closeBtnBg
            contentDescription = "Đóng"
            layoutParams = LinearLayout.LayoutParams(dp(context, 32), dp(context, 32))
            setOnClickListener { dismissOnMainThread() }
        }

        header.addView(titleStack)
        header.addView(closeBtn)
        root.addView(header)

        // Center illustration / custom image
        val imageSize = dp(context, 140)
        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(imageSize, imageSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(context, 12)
                bottomMargin = dp(context, 14)
            }
        }
        bindDeviceVisual(imageView, config, isDark)
        root.addView(imageView)

        // Battery section
        val batteryContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        currentBatteryContainer = batteryContainer
        renderBatterySection(context, batteryContainer, battery, isDark)
        root.addView(batteryContainer)

        return root
    }

    private fun bindDeviceVisual(
        imageView: ImageView,
        config: BluetoothPopupDeviceConfig,
        isDark: Boolean,
    ) {
        val customPath = config.customImagePath
        if (customPath != null) {
            val file = File(customPath)
            if (file.exists()) {
                try {
                    val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(file))
                    imageView.setImageDrawable(drawable)
                    if (drawable is AnimatedImageDrawable) {
                        drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        drawable.start()
                        currentAnimatedDrawable = drawable
                    }
                    return
                } catch (_: Exception) {
                }
            }
        }
        imageView.setImageDrawable(PresetIllustrationDrawable(config.preset, isDark))
    }

    private fun renderBatterySection(
        context: Context,
        container: LinearLayout,
        battery: BluetoothPopupBattery,
        isDark: Boolean,
    ) {
        container.removeAllViews()
        val secondaryText = if (isDark) 0xB3FFFFFF.toInt() else 0x9914161A.toInt()
        val primaryText = if (isDark) 0xFFF4F6FA.toInt() else 0xFF14161A.toInt()

        val hasLeftRight = battery.left in 0..100 || battery.right in 0..100
        if (hasLeftRight) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
            val items = mutableListOf<Pair<String, Int>>()
            if (battery.left in 0..100) items.add("Trái ${battery.left}%" to battery.left)
            if (battery.caseBattery in 0..100) items.add("Hộp ${battery.caseBattery}%" to battery.caseBattery)
            if (battery.right in 0..100) items.add("Phải ${battery.right}%" to battery.right)

            items.forEachIndexed { index, (label, level) ->
                val col = buildBatteryColumn(context, label, level, primaryText, isDark)
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (index > 0) marginStart = dp(context, 8)
                }
                row.addView(col, lp)
            }
            container.addView(row)
            return
        }

        if (battery.main in 0..100) {
            val col = buildBatteryColumn(
                context = context,
                label = "Pin: ${battery.main}%",
                level = battery.main,
                textColor = primaryText,
                isDark = isDark,
            )
            container.addView(col)
            return
        }

        val readyLabel = TextView(context).apply {
            text = "Âm thanh Bluetooth sẵn sàng"
            setTextColor(secondaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        container.addView(readyLabel)
    }

    private fun buildBatteryColumn(
        context: Context,
        label: String,
        level: Int,
        textColor: Int,
        isDark: Boolean,
    ): LinearLayout {
        val clamped = level.coerceIn(0, 100)
        val fillColor = if (clamped <= 15) COLOR_LOW_RED else COLOR_ACCENT_GREEN
        val trackColor = if (isDark) 0x24FFFFFF else 0x18000000

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val labelView = TextView(context).apply {
            text = label
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 6))
        }

        val trackHeight = dp(context, 6)
        val track = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                trackHeight,
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = trackHeight / 2f
                setColor(trackColor)
            }
        }

        val fillView = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = trackHeight / 2f
                setColor(fillColor)
            }
        }

        track.addView(
            fillView,
            FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        track.post {
            val w = track.width
            if (w > 0) {
                val fillW = (w * (clamped / 100f)).toInt().coerceAtLeast(trackHeight)
                val lp = fillView.layoutParams
                lp.width = fillW
                fillView.layoutParams = lp
            }
        }

        col.addView(labelView)
        col.addView(track)
        return col
    }

    private fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics,
        ).toInt()
    }

    private class PresetIllustrationDrawable(
        private val preset: BluetoothDevicePreset,
        private val isDark: Boolean,
    ) : Drawable() {

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

        override fun draw(canvas: Canvas) {
            val b = bounds
            val w = b.width().toFloat()
            val h = b.height().toFloat()
            if (w <= 0f || h <= 0f) return

            val bodyTop = if (isDark) 0xFF3E4451.toInt() else 0xFFE4E7EE.toInt()
            val bodyBottom = if (isDark) 0xFF232730.toInt() else 0xFFC5CBD6.toInt()
            val detailColor = if (isDark) 0xFF14171D.toInt() else 0xFF5A6270.toInt()
            val highlightStroke = if (isDark) 0x4DFFFFFF else 0x66FFFFFF

            fillPaint.shader = LinearGradient(
                0f, b.top.toFloat(), 0f, b.bottom.toFloat(),
                bodyTop, bodyBottom, Shader.TileMode.CLAMP,
            )

            when (preset) {
                BluetoothDevicePreset.TWS_EARBUDS -> drawTws(canvas, w, h, detailColor, highlightStroke)
                BluetoothDevicePreset.OVER_EAR -> drawOverEar(canvas, w, h, detailColor, highlightStroke)
                BluetoothDevicePreset.SPEAKER -> drawSpeaker(canvas, w, h, detailColor, highlightStroke)
                BluetoothDevicePreset.WATCH -> drawWatch(canvas, w, h, detailColor, highlightStroke)
            }
        }

        private fun drawTws(
            canvas: Canvas,
            w: Float,
            h: Float,
            detailColor: Int,
            highlightStroke: Int,
        ) {
            // Charging case
            val caseRect = RectF(w * 0.20f, h * 0.44f, w * 0.80f, h * 0.86f)
            canvas.drawRoundRect(caseRect, w * 0.14f, w * 0.14f, fillPaint)

            strokePaint.color = highlightStroke
            strokePaint.strokeWidth = w * 0.015f
            canvas.drawRoundRect(caseRect, w * 0.14f, w * 0.14f, strokePaint)

            // Lid seam
            strokePaint.color = detailColor
            strokePaint.strokeWidth = w * 0.014f
            canvas.drawLine(w * 0.22f, h * 0.57f, w * 0.78f, h * 0.57f, strokePaint)

            // Status LED
            fillPaint.shader = null
            fillPaint.color = COLOR_ACCENT_GREEN
            canvas.drawCircle(w * 0.50f, h * 0.65f, w * 0.024f, fillPaint)

            // Left & Right earbuds floating above case
            val bodyTop = if (isDark) 0xFFF0F3F8.toInt() else 0xFFFFFFFF.toInt()
            val bodyBottom = if (isDark) 0xFFB8C0CE.toInt() else 0xFFD4DAE4.toInt()
            fillPaint.shader = LinearGradient(0f, h * 0.14f, 0f, h * 0.46f, bodyTop, bodyBottom, Shader.TileMode.CLAMP)

            // Left earbud
            val leftHead = RectF(w * 0.27f, h * 0.15f, w * 0.44f, h * 0.30f)
            canvas.drawOval(leftHead, fillPaint)
            val leftStem = RectF(w * 0.34f, h * 0.26f, w * 0.40f, h * 0.43f)
            canvas.drawRoundRect(leftStem, w * 0.03f, w * 0.03f, fillPaint)

            // Right earbud
            val rightHead = RectF(w * 0.56f, h * 0.15f, w * 0.73f, h * 0.30f)
            canvas.drawOval(rightHead, fillPaint)
            val rightStem = RectF(w * 0.60f, h * 0.26f, w * 0.66f, h * 0.43f)
            canvas.drawRoundRect(rightStem, w * 0.03f, w * 0.03f, fillPaint)
        }

        private fun drawOverEar(
            canvas: Canvas,
            w: Float,
            h: Float,
            detailColor: Int,
            highlightStroke: Int,
        ) {
            // Headband arc
            strokePaint.color = if (isDark) 0xFF4A5160.toInt() else 0xFFCBD2DF.toInt()
            strokePaint.strokeWidth = w * 0.075f
            strokePaint.strokeCap = Paint.Cap.ROUND
            val arcRect = RectF(w * 0.20f, h * 0.16f, w * 0.80f, h * 0.72f)
            canvas.drawArc(arcRect, 185f, 170f, false, strokePaint)

            // Left & Right earcups
            val leftCup = RectF(w * 0.14f, h * 0.46f, w * 0.33f, h * 0.80f)
            val rightCup = RectF(w * 0.67f, h * 0.46f, w * 0.86f, h * 0.80f)
            canvas.drawRoundRect(leftCup, w * 0.09f, w * 0.09f, fillPaint)
            canvas.drawRoundRect(rightCup, w * 0.09f, w * 0.09f, fillPaint)

            // Inner cushion detail
            fillPaint.shader = null
            fillPaint.color = detailColor
            val leftInner = RectF(w * 0.26f, h * 0.50f, w * 0.33f, h * 0.76f)
            val rightInner = RectF(w * 0.67f, h * 0.50f, w * 0.74f, h * 0.76f)
            canvas.drawRoundRect(leftInner, w * 0.04f, w * 0.04f, fillPaint)
            canvas.drawRoundRect(rightInner, w * 0.04f, w * 0.04f, fillPaint)

            strokePaint.color = highlightStroke
            strokePaint.strokeWidth = w * 0.012f
            canvas.drawRoundRect(leftCup, w * 0.09f, w * 0.09f, strokePaint)
            canvas.drawRoundRect(rightCup, w * 0.09f, w * 0.09f, strokePaint)
        }

        private fun drawSpeaker(
            canvas: Canvas,
            w: Float,
            h: Float,
            detailColor: Int,
            highlightStroke: Int,
        ) {
            val bodyRect = RectF(w * 0.26f, h * 0.14f, w * 0.74f, h * 0.86f)
            canvas.drawRoundRect(bodyRect, w * 0.12f, w * 0.12f, fillPaint)

            strokePaint.color = highlightStroke
            strokePaint.strokeWidth = w * 0.015f
            canvas.drawRoundRect(bodyRect, w * 0.12f, w * 0.12f, strokePaint)

            // Woofer circles
            fillPaint.shader = null
            fillPaint.color = detailColor
            canvas.drawCircle(w * 0.50f, h * 0.40f, w * 0.13f, fillPaint)
            canvas.drawCircle(w * 0.50f, h * 0.68f, w * 0.10f, fillPaint)

            strokePaint.color = highlightStroke
            strokePaint.strokeWidth = w * 0.015f
            canvas.drawCircle(w * 0.50f, h * 0.40f, w * 0.08f, strokePaint)

            // Top status LED
            fillPaint.color = COLOR_ACCENT_GREEN
            canvas.drawCircle(w * 0.50f, h * 0.21f, w * 0.022f, fillPaint)
        }

        private fun drawWatch(
            canvas: Canvas,
            w: Float,
            h: Float,
            detailColor: Int,
            highlightStroke: Int,
        ) {
            // Top & bottom straps
            fillPaint.shader = null
            fillPaint.color = if (isDark) 0xFF2B303B.toInt() else 0xFFAEB6C4.toInt()
            val topStrap = RectF(w * 0.36f, h * 0.12f, w * 0.64f, h * 0.34f)
            val bottomStrap = RectF(w * 0.36f, h * 0.66f, w * 0.64f, h * 0.88f)
            canvas.drawRoundRect(topStrap, w * 0.06f, w * 0.06f, fillPaint)
            canvas.drawRoundRect(bottomStrap, w * 0.06f, w * 0.06f, fillPaint)

            // Watch case
            val caseRect = RectF(w * 0.27f, h * 0.28f, w * 0.73f, h * 0.72f)
            val bodyTop = if (isDark) 0xFF4B5262.toInt() else 0xFFE8ECF2.toInt()
            val bodyBottom = if (isDark) 0xFF242832.toInt() else 0xFFC5CCD8.toInt()
            fillPaint.shader = LinearGradient(0f, h * 0.28f, 0f, h * 0.72f, bodyTop, bodyBottom, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(caseRect, w * 0.11f, w * 0.11f, fillPaint)

            // Side crown
            fillPaint.shader = null
            fillPaint.color = bodyTop
            canvas.drawRoundRect(RectF(w * 0.73f, h * 0.43f, w * 0.77f, h * 0.53f), w * 0.02f, w * 0.02f, fillPaint)

            // Screen dial
            fillPaint.color = detailColor
            val dialRect = RectF(w * 0.31f, h * 0.32f, w * 0.69f, h * 0.68f)
            canvas.drawRoundRect(dialRect, w * 0.08f, w * 0.08f, fillPaint)

            // Activity ring
            strokePaint.color = COLOR_ACCENT_GREEN
            strokePaint.strokeWidth = w * 0.024f
            strokePaint.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(RectF(w * 0.39f, h * 0.39f, w * 0.61f, h * 0.61f), -90f, 270f, false, strokePaint)

            strokePaint.color = highlightStroke
            strokePaint.strokeWidth = w * 0.012f
            canvas.drawRoundRect(caseRect, w * 0.11f, w * 0.11f, strokePaint)
        }

        override fun setAlpha(alpha: Int) {
            fillPaint.alpha = alpha
            strokePaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            fillPaint.colorFilter = colorFilter
            strokePaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
