package com.originisle.android.bluetooth

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
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
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.PathInterpolator
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.originisle.android.R
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.min

data class BluetoothPopupBattery(
    val main: Int = -1,
    val left: Int = -1,
    val right: Int = -1,
    val caseBattery: Int = -1,
)

object BluetoothPopupOverlay {

    private const val COOLDOWN_MS = 12_000L
    private const val AUTO_DISMISS_MS = 6_000L
    private const val ENTER_SHEET_ANIM_MS = 300L
    private const val ENTER_SCRIM_ANIM_MS = 240L
    private const val ENTER_IMAGE_ANIM_MS = 320L
    private const val EXIT_ANIM_MS = 200L

    private const val COLOR_BATTERY_GREEN = 0xFF00C853.toInt()
    private const val COLOR_BATTERY_AMBER = 0xFFFF9F2E.toInt()
    private const val COLOR_BATTERY_RED = 0xFFFF5247.toInt()

    private val originInterpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lastShownElapsedMs = ConcurrentHashMap<String, Long>()

    private var currentView: View? = null
    private var currentRootScrim: FrameLayout? = null
    private var currentSheetContainer: SwipeDismissSheetLayout? = null
    private var currentWindowManager: WindowManager? = null
    private var currentKey: String? = null
    private var currentAnimatedDrawable: AnimatedImageDrawable? = null
    private var currentWebView: WebView? = null
    private var currentBatteryContainer: LinearLayout? = null
    private var currentIsDark: Boolean = false
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
        val isDark = resolveIsDarkTheme(context)
        val metrics = wm.currentWindowMetrics
        val screenWidth = metrics.bounds.width()
        val navBarBottom = metrics.windowInsets
            .getInsets(WindowInsets.Type.navigationBars())
            .bottom

        val rootScrim = FrameLayout(context).apply {
            setBackgroundColor(0x52000000)
            alpha = 0f
            setOnClickListener { dismissOnMainThread() }
        }

        val sheetContainer = buildSheetView(
            context = context,
            config = config,
            deviceName = deviceName,
            battery = battery,
            isDark = isDark,
            screenWidth = screenWidth,
            navBarBottom = navBarBottom,
            rootScrim = rootScrim,
        )

        val maxSheetWidth = dp(context, 480)
        val sheetWidth = if (screenWidth > maxSheetWidth) {
            min(screenWidth, maxSheetWidth)
        } else {
            ViewGroup.LayoutParams.MATCH_PARENT
        }

        rootScrim.addView(
            sheetContainer,
            FrameLayout.LayoutParams(
                sheetWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ),
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
        }

        try {
            wm.addView(rootScrim, params)
        } catch (_: Exception) {
            cleanupReferences()
            return
        }

        currentView = rootScrim
        currentRootScrim = rootScrim
        currentSheetContainer = sheetContainer
        currentWindowManager = wm
        currentKey = key
        currentIsDark = isDark
        isDismissing = false

        if (!forcePreview) {
            lastShownElapsedMs[key] = SystemClock.elapsedRealtime()
        }

        rootScrim.animate()
            .alpha(1f)
            .setDuration(ENTER_SCRIM_ANIM_MS)
            .start()

        sheetContainer.translationY = dp(context, 320).toFloat()
        sheetContainer.animate()
            .translationY(0f)
            .setDuration(ENTER_SHEET_ANIM_MS)
            .setInterpolator(originInterpolator)
            .start()

        scheduleAutoDismiss()
    }

    private fun resolveIsDarkTheme(context: Context): Boolean {
        return when (BluetoothPopupStore.getPopupTheme(context)) {
            BluetoothPopupTheme.LIGHT -> false
            BluetoothPopupTheme.DARK -> true
            BluetoothPopupTheme.SYSTEM -> {
                (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun updateBatteryOnMainThread(key: String, battery: BluetoothPopupBattery) {
        val view = currentView ?: return
        val container = currentBatteryContainer ?: return
        if (currentKey != key || isDismissing) return
        renderBatterySection(view.context, container, battery, currentIsDark)
    }

    private fun dismissOnMainThread() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        val rootView = currentView ?: return
        val sheet = currentSheetContainer
        if (isDismissing) return
        isDismissing = true

        rootView.animate().cancel()
        rootView.animate()
            .alpha(0f)
            .setDuration(EXIT_ANIM_MS)
            .start()

        val targetView = sheet ?: rootView
        targetView.animate().cancel()
        targetView.animate()
            .translationY(dp(rootView.context, 320).toFloat())
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
        val sheet = currentSheetContainer
        val wm = currentWindowManager
        sheet?.animate()?.cancel()
        if (view != null) {
            view.animate().cancel()
            view.setOnClickListener(null)
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
        currentWebView?.apply {
            onPause()
            stopLoading()
            loadUrl("about:blank")
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        currentWebView = null
        currentView = null
        currentRootScrim = null
        currentSheetContainer = null
        currentWindowManager = null
        currentKey = null
        currentBatteryContainer = null
        isDismissing = false
    }

    private fun scheduleAutoDismiss() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        mainHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_MS)
    }

    private fun pauseAutoDismiss() {
        mainHandler.removeCallbacks(autoDismissRunnable)
    }

    private fun buildSheetView(
        context: Context,
        config: BluetoothPopupDeviceConfig,
        deviceName: String,
        battery: BluetoothPopupBattery,
        isDark: Boolean,
        screenWidth: Int,
        navBarBottom: Int,
        rootScrim: FrameLayout,
    ): SwipeDismissSheetLayout {
        val cornerRadius = dp(context, 32).toFloat()
        val sheetBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(
                cornerRadius, cornerRadius,
                cornerRadius, cornerRadius,
                0f, 0f,
                0f, 0f,
            )
            setColor(if (isDark) 0xFF1E2026.toInt() else 0xFFFFFFFF.toInt())
            if (isDark) {
                setStroke(dp(context, 1), 0x1FFFFFFF)
            }
        }

        val sheet = SwipeDismissSheetLayout(context, rootScrim).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = sheetBg
            elevation = dp(context, 24).toFloat()
            setPadding(0, 0, 0, navBarBottom + dp(context, 16))
            isClickable = true
            isFocusable = false
            // Keep screenWidth referenced cleanly for tablet/landscape sizing symmetry
            minimumWidth = min(screenWidth, dp(context, 320))
        }

        // OriginOS slim drag handle
        val handleView = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                this.cornerRadius = dp(context, 2).toFloat()
                setColor(if (isDark) 0xFF3A3F4B.toInt() else 0xFFE0E3E8.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(dp(context, 36), dp(context, 4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(context, 10)
            }
        }
        sheet.addView(handleView)

        val resolvedName = config.customName?.takeIf { it.isNotBlank() }
            ?: deviceName.ifBlank { config.preset.label }

        // Title
        val titleView = TextView(context).apply {
            text = resolvedName
            gravity = Gravity.CENTER
            setTextColor(if (isDark) 0xFFF3F5F8.toInt() else 0xFF191C20.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = -0.01f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(context, 28), 0, dp(context, 28), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(context, 18)
            }
        }
        sheet.addView(titleView)

        // Subtitle
        val subtitleView = TextView(context).apply {
            text = "$resolvedName đã kết nối và sẵn sàng phát âm thanh trên thiết bị này"
            gravity = Gravity.CENTER
            setTextColor(if (isDark) 0xFF9AA0AA.toInt() else 0xFF6E737B.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setLineSpacing(dp(context, 3).toFloat(), 1f)
            setPadding(dp(context, 32), 0, dp(context, 32), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(context, 6)
            }
        }
        sheet.addView(subtitleView)

        // Large 3D device visual
        val visualView = buildDeviceVisualView(context, config, isDark)
        visualView.scaleX = 0.92f
        visualView.scaleY = 0.92f
        visualView.alpha = 0f
        visualView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(ENTER_IMAGE_ANIM_MS)
            .setInterpolator(originInterpolator)
            .start()
        sheet.addView(visualView)

        // OriginOS battery pills
        val batteryContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(context, 20), 0, dp(context, 20), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(context, 18)
            }
        }
        currentBatteryContainer = batteryContainer
        renderBatterySection(context, batteryContainer, battery, isDark)
        sheet.addView(batteryContainer)

        // Bottom action row
        val actionRow = FrameLayout(context).apply {
            setPadding(dp(context, 24), 0, dp(context, 24), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 52),
            )
        }

        val infoStrokeColor = if (isDark) 0xFF9AA0AA.toInt() else 0xFF74777F.toInt()
        val infoBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (isDark) 0x14FFFFFF else 0x0A000000)
            setStroke(dpFloat(context, 1.3f).toInt().coerceAtLeast(1), infoStrokeColor)
        }
        val infoButton = TextView(context).apply {
            text = "i"
            gravity = Gravity.CENTER
            setTextColor(infoStrokeColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            background = infoBg
            contentDescription = "Cài đặt Bluetooth"
            layoutParams = FrameLayout.LayoutParams(
                dp(context, 34),
                dp(context, 34),
                Gravity.START or Gravity.CENTER_VERTICAL,
            )
            setOnClickListener {
                dismissOnMainThread()
                runCatching {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        }
        actionRow.addView(infoButton)

        val doneBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = dp(context, 23).toFloat()
            setColor(if (isDark) 0xFF2B3E6E.toInt() else 0xFFDCE6FF.toInt())
        }
        val doneButton = TextView(context).apply {
            text = "Hoàn tất"
            gravity = Gravity.CENTER
            setTextColor(if (isDark) 0xFFDCE6FF.toInt() else 0xFF1B3A7A.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = doneBg
            layoutParams = FrameLayout.LayoutParams(
                dp(context, 204),
                dp(context, 46),
                Gravity.CENTER,
            )
            setOnClickListener { dismissOnMainThread() }
        }
        actionRow.addView(doneButton)

        sheet.addView(actionRow)
        return sheet
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun buildDeviceVisualView(
        context: Context,
        config: BluetoothPopupDeviceConfig,
        isDark: Boolean,
    ): View {
        if (config.customImagePath == null && config.preset.isInteractive3D) {
            try {
                val themeParam = if (isDark) "dark" else "light"
                val webView = WebView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        dp(context, 224),
                        dp(context, 204),
                    ).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        topMargin = dp(context, 10)
                        bottomMargin = dp(context, 10)
                    }
                    setBackgroundColor(Color.TRANSPARENT)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isClickable = false
                    isFocusable = false
                    setOnTouchListener { _, _ -> false }
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    settings.domStorageEnabled = false
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.setSupportZoom(false)
                    loadUrl(
                        "file:///android_asset/bt3d/viewer.html?model=${config.preset.id}&theme=$themeParam",
                    )
                }
                currentWebView = webView
                return webView
            } catch (_: Throwable) {
                currentWebView = null
            }
        }

        return ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(dp(context, 210), dp(context, 192)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(context, 14)
                bottomMargin = dp(context, 14)
            }
            bindDeviceVisual(this, config)
        }
    }

    private fun bindDeviceVisual(
        imageView: ImageView,
        config: BluetoothPopupDeviceConfig,
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
        imageView.setImageResource(presetDrawableRes(config.preset))
    }

    private fun presetDrawableRes(preset: BluetoothDevicePreset): Int = when (preset) {
        BluetoothDevicePreset.TWS_EARBUDS -> R.drawable.bt_preset_tws_dark_3d
        BluetoothDevicePreset.TWS_WHITE -> R.drawable.bt_preset_tws_white_3d
        BluetoothDevicePreset.LENOVO_LE208 -> R.drawable.bt_preset_lenovo_le208_3d
        BluetoothDevicePreset.REDMI_BUDS_6_PLAY -> R.drawable.bt_preset_redmi_buds_6_play_3d
        BluetoothDevicePreset.OVER_EAR -> R.drawable.bt_preset_over_ear_3d
        BluetoothDevicePreset.SPEAKER -> R.drawable.bt_preset_speaker_3d
        BluetoothDevicePreset.WATCH -> R.drawable.bt_preset_watch_3d
    }

    private fun renderBatterySection(
        context: Context,
        container: LinearLayout,
        battery: BluetoothPopupBattery,
        isDark: Boolean,
    ) {
        container.removeAllViews()

        val items = mutableListOf<Pair<String, Int>>()
        val hasLeftRight = battery.left in 0..100 || battery.right in 0..100
        if (hasLeftRight) {
            if (battery.left in 0..100) items.add("L ${battery.left}%" to battery.left)
            if (battery.caseBattery in 0..100) items.add("Hộp ${battery.caseBattery}%" to battery.caseBattery)
            if (battery.right in 0..100) items.add("R ${battery.right}%" to battery.right)
        } else if (battery.main in 0..100) {
            items.add("Pin ${battery.main}%" to battery.main)
        }

        if (items.isEmpty()) {
            container.visibility = View.GONE
            return
        }

        container.visibility = View.VISIBLE
        val pillBgColor = if (isDark) 0xFF2A2E37.toInt() else 0xFFF2F4F8.toInt()
        val textColor = if (isDark) 0xFFE5E7EB.toInt() else 0xFF1F2937.toInt()

        items.forEach { (label, level) ->
            val pill = buildBatteryPill(context, label, level, pillBgColor, textColor, isDark)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(context, 28),
            ).apply {
                marginStart = dp(context, 4)
                marginEnd = dp(context, 4)
            }
            container.addView(pill, lp)
        }
    }

    private fun buildBatteryPill(
        context: Context,
        label: String,
        level: Int,
        pillBgColor: Int,
        textColor: Int,
        isDark: Boolean,
    ): LinearLayout {
        val pill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 10), 0, dp(context, 10), 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(context, 999).toFloat()
                setColor(pillBgColor)
            }
        }

        val batteryIcon = View(context).apply {
            background = BatteryPillIconDrawable(level.coerceIn(0, 100), isDark)
            layoutParams = LinearLayout.LayoutParams(dp(context, 20), dp(context, 10))
        }
        pill.addView(batteryIcon)

        val labelView = TextView(context).apply {
            text = label
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = dp(context, 5)
            }
        }
        pill.addView(labelView)

        return pill
    }

    private fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics,
        ).toInt()
    }

    private fun dpFloat(context: Context, value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics,
        )
    }

    private class SwipeDismissSheetLayout(
        context: Context,
        private val rootScrim: FrameLayout,
    ) : LinearLayout(context) {

        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private val dismissThresholdPx = dp(context, 72).toFloat()
        private val scrimFadeDistancePx = dp(context, 280).toFloat()

        private var downX = 0f
        private var downY = 0f
        private var isDragging = false
        private var velocityTracker: VelocityTracker? = null

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    isDragging = false
                    pauseAutoDismiss()
                    initVelocityTracker(ev)
                }
                MotionEvent.ACTION_MOVE -> {
                    trackVelocity(ev)
                    val dy = ev.rawY - downY
                    val dx = abs(ev.rawX - downX)
                    if (dy > touchSlop && dy > dx) {
                        isDragging = true
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        recycleVelocityTracker()
                        scheduleAutoDismiss()
                    }
                }
            }
            return false
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    isDragging = false
                    pauseAutoDismiss()
                    initVelocityTracker(event)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    trackVelocity(event)
                    val dy = (event.rawY - downY).coerceAtLeast(0f)
                    if (dy > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        translationY = dy
                        val progress = (dy / scrimFadeDistancePx).coerceIn(0f, 1f)
                        rootScrim.alpha = 1f - progress * 0.85f
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    trackVelocity(event)
                    val dy = (event.rawY - downY).coerceAtLeast(0f)
                    val yVelocity = velocityTracker?.let { tracker ->
                        tracker.computeCurrentVelocity(1000)
                        tracker.yVelocity
                    } ?: 0f
                    recycleVelocityTracker()

                    if (isDragging && (dy > dismissThresholdPx || yVelocity > 1200f)) {
                        dismissOnMainThread()
                    } else {
                        animate()
                            .translationY(0f)
                            .setDuration(220L)
                            .setInterpolator(originInterpolator)
                            .start()
                        rootScrim.animate()
                            .alpha(1f)
                            .setDuration(220L)
                            .start()
                        scheduleAutoDismiss()
                    }
                    isDragging = false
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        private fun initVelocityTracker(event: MotionEvent) {
            velocityTracker?.recycle()
            velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
        }

        private fun trackVelocity(event: MotionEvent) {
            velocityTracker?.addMovement(event)
        }

        private fun recycleVelocityTracker() {
            velocityTracker?.recycle()
            velocityTracker = null
        }
    }

    private class BatteryPillIconDrawable(
        private val level: Int,
        private val isDark: Boolean,
    ) : Drawable() {

        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        override fun draw(canvas: Canvas) {
            val b = bounds
            val w = b.width().toFloat()
            val h = b.height().toFloat()
            if (w <= 0f || h <= 0f) return

            val strokeWidth = (h * 0.12f).coerceAtLeast(1.5f)
            val halfStroke = strokeWidth / 2f
            val nubWidth = w * 0.08f
            val nubGap = w * 0.05f
            val bodyRight = w - nubWidth - nubGap

            val borderColor = if (isDark) 0xFF9AA0AA.toInt() else 0xFF74777F.toInt()
            strokePaint.color = borderColor
            strokePaint.strokeWidth = strokeWidth

            val bodyRect = RectF(
                halfStroke,
                halfStroke,
                bodyRight - halfStroke,
                h - halfStroke,
            )
            val cornerRadius = h * 0.24f
            canvas.drawRoundRect(bodyRect, cornerRadius, cornerRadius, strokePaint)

            // Positive terminal nub
            fillPaint.color = borderColor
            val nubRect = RectF(
                bodyRight + nubGap * 0.5f,
                h * 0.30f,
                w,
                h * 0.70f,
            )
            canvas.drawRoundRect(nubRect, nubWidth * 0.5f, nubWidth * 0.5f, fillPaint)

            // Inner battery charge bar
            val inset = strokeWidth + h * 0.10f
            val innerLeft = inset
            val innerTop = inset
            val innerRightMax = bodyRight - inset
            val innerBottom = h - inset
            val innerWidthMax = (innerRightMax - innerLeft).coerceAtLeast(0f)

            if (level > 0 && innerWidthMax > 0f) {
                val fillWidth = (innerWidthMax * (level / 100f))
                    .coerceAtLeast(h * 0.18f)
                    .coerceAtMost(innerWidthMax)
                fillPaint.color = when {
                    level <= 15 -> COLOR_BATTERY_RED
                    level <= 20 -> COLOR_BATTERY_AMBER
                    else -> COLOR_BATTERY_GREEN
                }
                val fillRect = RectF(
                    innerLeft,
                    innerTop,
                    innerLeft + fillWidth,
                    innerBottom,
                )
                val innerRadius = ((innerBottom - innerTop) * 0.28f).coerceAtLeast(1f)
                canvas.drawRoundRect(fillRect, innerRadius, innerRadius, fillPaint)
            }
        }

        override fun setAlpha(alpha: Int) {
            strokePaint.alpha = alpha
            fillPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            strokePaint.colorFilter = colorFilter
            fillPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
