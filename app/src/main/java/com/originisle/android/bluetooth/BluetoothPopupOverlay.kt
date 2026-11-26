package com.originisle.android.bluetooth

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.originisle.android.R
import com.originisle.android.ui.resolveAppIsDarkTheme
import com.originisle.android.ui.resolveMaterialColorScheme
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
    private const val COLOR_STATUS_CONNECTED = 0xFF00D084.toInt()
    private const val BLUR_BEHIND_RADIUS_PX = 36

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
    private val activeAnimators = mutableListOf<Animator>()
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
        if (!forcePreview && !BluetoothPopupStore.isHeadphoneNameAllowed(deviceName)) return false
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
        val screenHeight = metrics.bounds.height()
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
            rootScrim = rootScrim,
        )

        val sideMarginPx = dp(context, 12)
        val isWideOrFoldable = screenWidth > screenHeight * 4 / 3 || screenWidth > dp(context, 480)
        val sheetWidth = if (isWideOrFoldable) {
            min(screenWidth - sideMarginPx * 2, dp(context, 420))
        } else {
            ViewGroup.LayoutParams.MATCH_PARENT
        }

        val sheetLayoutParams = FrameLayout.LayoutParams(
            sheetWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        ).apply {
            leftMargin = sideMarginPx
            rightMargin = sideMarginPx
            bottomMargin = navBarBottom + dp(context, 12)
        }

        rootScrim.addView(sheetContainer, sheetLayoutParams)

        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        val windowFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            baseFlags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        } else {
            baseFlags
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            windowFlags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurBehindRadius = BLUR_BEHIND_RADIUS_PX
            }
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

        sheetContainer.translationY = dp(context, 260).toFloat()
        sheetContainer.scaleX = 0.96f
        sheetContainer.scaleY = 0.96f
        sheetContainer.animate()
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ENTER_SHEET_ANIM_MS)
            .setInterpolator(originInterpolator)
            .start()

        scheduleAutoDismiss()
    }

    private fun resolveIsDarkTheme(context: Context): Boolean {
        return when (BluetoothPopupStore.getPopupTheme(context)) {
            BluetoothPopupTheme.LIGHT -> false
            BluetoothPopupTheme.DARK -> true
            BluetoothPopupTheme.SYSTEM -> resolveAppIsDarkTheme(context)
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
            .translationY(dp(rootView.context, 260).toFloat())
            .scaleX(0.96f)
            .scaleY(0.96f)
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
        activeAnimators.forEach { runCatching { it.cancel() } }
        activeAnimators.clear()
        currentAnimatedDrawable?.stop()
        currentAnimatedDrawable = null
        currentWebView?.let { wv ->
            runCatching { wv.removeJavascriptInterface("AndroidBridge") }
            runCatching { wv.stopLoading() }
            runCatching { wv.loadUrl("about:blank") }
            runCatching { wv.onPause() }
            runCatching { wv.removeAllViews() }
            runCatching { wv.destroy() }
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
        rootScrim: FrameLayout,
    ): SwipeDismissSheetLayout {
        val colorScheme = resolveMaterialColorScheme(context, isDark)
        val cornerRadius = dp(context, 32).toFloat()
        val glassBgColor = if (isDark) 0xEB141926.toInt() else 0xF2FFFFFF.toInt()
        val edgeLightTop = if (isDark) 0x42FFFFFF else 0xE6FFFFFF.toInt()
        val edgeLightBottom = if (isDark) 0x0DFFFFFF else 0x14000000
        val specularColor = if (isDark) 0x66FFFFFF else 0xFAFFFFFF.toInt()
        val ambientColor = colorScheme.primary.copy(alpha = if (isDark) 0.16f else 0.10f).toArgb()
        val sheetBg = LiquidGlassSheetDrawable(
            cornerRadiusPx = cornerRadius,
            fillColor = glassBgColor,
            ambientColor = ambientColor,
            edgeTopColor = edgeLightTop,
            edgeBottomColor = edgeLightBottom,
            specularColor = specularColor,
            strokeWidthPx = dpFloat(context, 1f).coerceAtLeast(1f),
        )

        val sheet = SwipeDismissSheetLayout(context, rootScrim).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = sheetBg
            elevation = dp(context, 24).toFloat()
            setPadding(0, 0, 0, dp(context, 20))
            isClickable = true
            isFocusable = false
            // Keep screenWidth referenced cleanly for tablet/landscape sizing symmetry
            minimumWidth = min((screenWidth - dp(context, 24)).coerceAtLeast(0), dp(context, 320))
        }

        // OriginOS slim drag handle
        val handleView = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                this.cornerRadius = dp(context, 2).toFloat()
                setColor(colorScheme.onSurfaceVariant.copy(alpha = 0.38f).toArgb())
            }
            layoutParams = LinearLayout.LayoutParams(dp(context, 36), dp(context, 4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(context, 10)
            }
        }
        sheet.addView(handleView)

        // OriginOS Liquid Glass connected status pill
        val statusPillBg = LiquidGlassSheetDrawable(
            cornerRadiusPx = dp(context, 999).toFloat(),
            fillColor = if (isDark) 0x1AFFFFFF else 0xA8F2F4F8.toInt(),
            ambientColor = Color.TRANSPARENT,
            edgeTopColor = if (isDark) 0x38FFFFFF else 0xC8FFFFFF.toInt(),
            edgeBottomColor = if (isDark) 0x0AFFFFFF else 0x14000000,
            specularColor = Color.TRANSPARENT,
            strokeWidthPx = dpFloat(context, 0.8f).coerceAtLeast(1f),
        )
        val statusPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = statusPillBg
            setPadding(dp(context, 10), dp(context, 4), dp(context, 11), dp(context, 4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(context, 12)
            }
        }
        val statusDot = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(COLOR_STATUS_CONNECTED)
            }
            layoutParams = LinearLayout.LayoutParams(dp(context, 7), dp(context, 7))
        }
        val statusLabel = TextView(context).apply {
            text = "Đã kết nối"
            setTextColor(colorScheme.onSurface.toArgb())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            includeFontPadding = true
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = dp(context, 6)
            }
        }
        statusPill.addView(statusDot)
        statusPill.addView(statusLabel)
        sheet.addView(statusPill)

        val resolvedName = config.customName?.takeIf { it.isNotBlank() }
            ?: deviceName.ifBlank { config.preset.label }

        // Title (vivo Sans / Vietnamese diacritic-safe line height & font padding)
        val titleView = TextView(context).apply {
            text = resolvedName
            gravity = Gravity.CENTER
            setTextColor(colorScheme.onSurface.toArgb())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            includeFontPadding = true
            setLineSpacing(dpFloat(context, 4f), 1.05f)
            letterSpacing = -0.01f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(context, 28), dp(context, 2), dp(context, 28), dp(context, 2))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(context, 10)
            }
        }
        sheet.addView(titleView)

        // Subtitle
        val subtitleView = TextView(context).apply {
            text = "$resolvedName đã kết nối và sẵn sàng phát âm thanh trên thiết bị này"
            gravity = Gravity.CENTER
            setTextColor(colorScheme.onSurfaceVariant.toArgb())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            includeFontPadding = true
            setLineSpacing(dpFloat(context, 3f), 1.05f)
            setPadding(dp(context, 30), 0, dp(context, 30), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(context, 4)
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

        // OriginOS Huarong Atomic battery pods
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

        val infoStrokeColor = colorScheme.onSurfaceVariant.toArgb()
        val infoGlassFill = if (isDark) 0x18FFFFFF else 0xB8F2F4F8.toInt()
        val infoEdgeStroke = if (isDark) 0x30FFFFFF else 0x1F000000
        val infoBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(infoGlassFill)
            setStroke(dpFloat(context, 1f).toInt().coerceAtLeast(1), infoEdgeStroke)
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
                dp(context, 36),
                dp(context, 36),
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
        attachTouchLightFeedback(infoButton)
        actionRow.addView(infoButton)

        val doneBgColor = if (isDark) 0xFFFFFFFF.toInt() else colorScheme.primary.toArgb()
        val doneTextColor = if (isDark) 0xFF121620.toInt() else colorScheme.onPrimary.toArgb()
        val doneBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = dp(context, 999).toFloat()
            setColor(doneBgColor)
        }
        val doneButton = TextView(context).apply {
            text = "Hoàn tất"
            gravity = Gravity.CENTER
            setTextColor(doneTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            includeFontPadding = true
            background = doneBg
            layoutParams = FrameLayout.LayoutParams(
                dp(context, 204),
                dp(context, 46),
                Gravity.CENTER,
            )
            setOnClickListener { dismissOnMainThread() }
        }
        attachTouchLightFeedback(doneButton)
        actionRow.addView(doneButton)

        sheet.addView(actionRow)
        return sheet
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachTouchLightFeedback(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().cancel()
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .alpha(0.88f)
                        .setDuration(90L)
                        .setInterpolator(originInterpolator)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().cancel()
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(240L)
                        .setInterpolator(OvershootInterpolator(1.1f))
                        .start()
                }
            }
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildDeviceVisualView(
        context: Context,
        config: BluetoothPopupDeviceConfig,
        @Suppress("UNUSED_PARAMETER") isDark: Boolean,
    ): View {
        if (config.customImagePath == null) {
            when (config.preset) {
                BluetoothDevicePreset.LENOVO_LE208 -> return buildLenovoLe208PhotorealStage(context)
                BluetoothDevicePreset.REDMI_BUDS_6_PLAY -> return buildRedmi3dWebViewStage(context)
                else -> Unit
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

    private fun buildLenovoLe208PhotorealStage(context: Context): FrameLayout {
        val stageW = dp(context, 226)
        val stageH = dp(context, 226)
        val density = context.resources.displayMetrics.density
        val smoothOut = PathInterpolator(0.22f, 1f, 0.36f, 1f)

        val stage = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(stageW, stageH).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(context, 6)
                bottomMargin = dp(context, 8)
            }
            clipChildren = false
            clipToPadding = false
        }

        val baseView = ImageView(context).apply {
            setImageResource(R.drawable.bt_anim_le208_base)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            alpha = 0f
        }

        val lidView = ImageView(context).apply {
            setImageResource(R.drawable.bt_anim_le208_lid)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            cameraDistance = 14000f * density
            pivotX = stageW / 2f
            pivotY = stageH * 0.534f
            rotationX = 86f
            alpha = 0f
        }

        val closedCaseView = ImageView(context).apply {
            setImageResource(R.drawable.bt_anim_le208_closed)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            cameraDistance = 14000f * density
            pivotX = stageW / 2f
            pivotY = stageH * 0.60f
            translationY = dpFloat(context, 8f)
            alpha = 0f
        }

        val budW = dp(context, 82)
        val budH = dp(context, 98)
        val startBudY = dpFloat(context, 64f)
        val leftEndY = dpFloat(context, 22f)
        val rightEndY = dpFloat(context, 10f)

        val leftBudView = ImageView(context).apply {
            setImageResource(R.drawable.bt_anim_le208_bud_left)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(budW, budH).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            }
            translationX = -dpFloat(context, 31f)
            translationY = startBudY
            scaleX = 0.80f
            scaleY = 0.80f
            alpha = 0f
        }

        val rightBudView = ImageView(context).apply {
            setImageResource(R.drawable.bt_anim_le208_bud_right)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(budW, budH).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            }
            translationX = dpFloat(context, 31f)
            translationY = startBudY
            scaleX = 0.80f
            scaleY = 0.80f
            alpha = 0f
        }

        stage.addView(lidView)
        stage.addView(baseView)
        stage.addView(closedCaseView)
        stage.addView(leftBudView)
        stage.addView(rightBudView)

        val closedIntro = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(closedCaseView, View.ALPHA, 0f, 1f).setDuration(240L),
                ObjectAnimator.ofFloat(closedCaseView, View.TRANSLATION_Y, dpFloat(context, 8f), 0f).apply {
                    duration = 280L
                    interpolator = smoothOut
                },
            )
        }

        val openHingeAnim = AnimatorSet().apply {
            startDelay = 300L
            playTogether(
                ObjectAnimator.ofFloat(closedCaseView, View.ROTATION_X, 0f, -16f).setDuration(180L),
                ObjectAnimator.ofFloat(closedCaseView, View.ALPHA, 1f, 0f).apply {
                    startDelay = 30L
                    duration = 160L
                },
                ObjectAnimator.ofFloat(baseView, View.ALPHA, 0f, 1f).setDuration(150L),
                ObjectAnimator.ofFloat(lidView, View.ALPHA, 0f, 1f).setDuration(120L),
                ObjectAnimator.ofFloat(lidView, View.ROTATION_X, 86f, 0f).apply {
                    duration = 420L
                    interpolator = smoothOut
                },
            )
        }

        val liftLeftAnim = AnimatorSet().apply {
            startDelay = 540L
            playTogether(
                ObjectAnimator.ofFloat(leftBudView, View.ALPHA, 0f, 1f).setDuration(220L),
                ObjectAnimator.ofFloat(leftBudView, View.TRANSLATION_Y, startBudY, leftEndY).apply {
                    duration = 520L
                    interpolator = smoothOut
                },
                ObjectAnimator.ofFloat(leftBudView, View.SCALE_X, 0.80f, 1f).apply {
                    duration = 480L
                    interpolator = smoothOut
                },
                ObjectAnimator.ofFloat(leftBudView, View.SCALE_Y, 0.80f, 1f).apply {
                    duration = 480L
                    interpolator = smoothOut
                },
            )
        }

        val liftRightAnim = AnimatorSet().apply {
            startDelay = 600L
            playTogether(
                ObjectAnimator.ofFloat(rightBudView, View.ALPHA, 0f, 1f).setDuration(220L),
                ObjectAnimator.ofFloat(rightBudView, View.TRANSLATION_Y, startBudY, rightEndY).apply {
                    duration = 540L
                    interpolator = smoothOut
                },
                ObjectAnimator.ofFloat(rightBudView, View.SCALE_X, 0.80f, 1f).apply {
                    duration = 480L
                    interpolator = smoothOut
                },
                ObjectAnimator.ofFloat(rightBudView, View.SCALE_Y, 0.80f, 1f).apply {
                    duration = 480L
                    interpolator = smoothOut
                },
            )
        }

        listOf(closedIntro, openHingeAnim, liftLeftAnim, liftRightAnim).forEach {
            activeAnimators.add(it)
            it.start()
        }

        return stage
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun buildRedmi3dWebViewStage(context: Context): FrameLayout {
        val stage = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 212),
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(context, 6)
                bottomMargin = dp(context, 8)
            }
            clipChildren = false
            clipToPadding = false
        }

        runCatching {
            val webView = WebView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                alpha = 0f
                setBackgroundColor(Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                setOnTouchListener { _, _ -> false }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    allowFileAccess = false
                    allowContentAccess = false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val url = request?.url ?: return null
                        if (url.host != "bt3d.local") return super.shouldInterceptRequest(view, request)
                        val assetName = url.lastPathSegment?.takeIf { it.isNotBlank() } ?: "viewer.html"
                        val mimeType = when {
                            assetName.endsWith(".html", ignoreCase = true) -> "text/html"
                            assetName.endsWith(".js", ignoreCase = true) -> "application/javascript"
                            assetName.endsWith(".glb", ignoreCase = true) -> "model/gltf-binary"
                            else -> "application/octet-stream"
                        }
                        val stream = runCatching {
                            context.assets.open("bt3d/$assetName")
                        }.getOrNull() ?: return null
                        return WebResourceResponse(
                            mimeType,
                            "UTF-8",
                            200,
                            "OK",
                            mapOf("Access-Control-Allow-Origin" to "*"),
                            stream,
                        )
                    }
                }
            }

            webView.addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onReady() {
                        mainHandler.post {
                            if (currentWebView !== webView) return@post
                            webView.animate().alpha(1f).setDuration(140L).start()
                        }
                    }
                },
                "AndroidBridge",
            )

            currentWebView = webView
            stage.addView(webView)
            val colorId = BluetoothPopupStore.getRedmiCaseColor(context).id
            webView.loadUrl("https://bt3d.local/viewer.html?color=$colorId")
        }

        return stage
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
        val colorScheme = resolveMaterialColorScheme(context, isDark)
        val podBgColor = if (isDark) 0x1AFFFFFF else 0xC4F3F5FA.toInt()
        val podEdgeTop = if (isDark) 0x38FFFFFF else 0xD9FFFFFF.toInt()
        val podEdgeBottom = if (isDark) 0x0AFFFFFF else 0x16000000
        val textColor = colorScheme.onSurface.toArgb()

        items.forEach { (label, level) ->
            val pod = buildBatteryPill(
                context = context,
                label = label,
                level = level,
                podBgColor = podBgColor,
                podEdgeTop = podEdgeTop,
                podEdgeBottom = podEdgeBottom,
                textColor = textColor,
                isDark = isDark,
            )
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(context, 34),
            ).apply {
                marginStart = dp(context, 4)
                marginEnd = dp(context, 4)
            }
            container.addView(pod, lp)
        }
    }

    private fun buildBatteryPill(
        context: Context,
        label: String,
        level: Int,
        podBgColor: Int,
        podEdgeTop: Int,
        podEdgeBottom: Int,
        textColor: Int,
        isDark: Boolean,
    ): LinearLayout {
        val pod = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 12), dp(context, 4), dp(context, 12), dp(context, 4))
            background = LiquidGlassSheetDrawable(
                cornerRadiusPx = dp(context, 16).toFloat(),
                fillColor = podBgColor,
                ambientColor = Color.TRANSPARENT,
                edgeTopColor = podEdgeTop,
                edgeBottomColor = podEdgeBottom,
                specularColor = Color.TRANSPARENT,
                strokeWidthPx = dpFloat(context, 1f).coerceAtLeast(1f),
            )
        }

        val batteryIcon = View(context).apply {
            background = BatteryPillIconDrawable(level.coerceIn(0, 100), isDark)
            layoutParams = LinearLayout.LayoutParams(dp(context, 20), dp(context, 10))
        }
        pod.addView(batteryIcon)

        val labelView = TextView(context).apply {
            text = label
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            fontFeatureSettings = "tnum"
            includeFontPadding = true
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = dp(context, 6)
            }
        }
        pod.addView(labelView)

        return pod
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
        private var startTranslationY = 0f
        private var isDragging = false
        private var velocityTracker: VelocityTracker? = null

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    animate().cancel()
                    rootScrim.animate().cancel()
                    downX = ev.rawX
                    downY = ev.rawY
                    startTranslationY = translationY
                    isDragging = false
                    pauseAutoDismiss()
                    initVelocityTracker(ev)
                }
                MotionEvent.ACTION_MOVE -> {
                    trackVelocity(ev)
                    val dy = ev.rawY - downY
                    val absDy = abs(dy)
                    val dx = abs(ev.rawX - downX)
                    if (absDy > touchSlop && absDy > dx) {
                        isDragging = true
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        recycleVelocityTracker()
                        if (translationY != 0f || scaleX != 1f || scaleY != 1f || rootScrim.alpha < 1f) {
                            animate()
                                .translationY(0f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(220L)
                                .setInterpolator(originInterpolator)
                                .start()
                            rootScrim.animate()
                                .alpha(1f)
                                .setDuration(200L)
                                .start()
                        }
                        scheduleAutoDismiss()
                    }
                }
            }
            return false
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    animate().cancel()
                    rootScrim.animate().cancel()
                    downX = event.rawX
                    downY = event.rawY
                    startTranslationY = translationY
                    isDragging = false
                    pauseAutoDismiss()
                    initVelocityTracker(event)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    trackVelocity(event)
                    val deltaY = event.rawY - downY
                    val rawDy = startTranslationY + deltaY
                    if (abs(deltaY) > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        if (rawDy >= 0f) {
                            translationY = rawDy
                            val progress = (rawDy / scrimFadeDistancePx).coerceIn(0f, 1f)
                            rootScrim.alpha = 1f - progress * 0.85f
                        } else {
                            // Surface tension resistance when pulling slightly upward
                            translationY = rawDy * 0.18f
                            rootScrim.alpha = 1f
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    trackVelocity(event)
                    val rawDy = startTranslationY + (event.rawY - downY)
                    val yVelocity = velocityTracker?.let { tracker ->
                        tracker.computeCurrentVelocity(1000)
                        tracker.yVelocity
                    } ?: 0f
                    recycleVelocityTracker()

                    if (isDragging && (rawDy > dismissThresholdPx || (rawDy > 0f && yVelocity > 1200f))) {
                        dismissOnMainThread()
                    } else {
                        animate()
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(260L)
                            .setInterpolator(OvershootInterpolator(0.85f))
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

    private class LiquidGlassSheetDrawable(
        private val cornerRadiusPx: Float,
        fillColor: Int,
        private val ambientColor: Int,
        private val edgeTopColor: Int,
        private val edgeBottomColor: Int,
        private val specularColor: Int,
        private val strokeWidthPx: Float,
    ) : Drawable() {

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColor
        }
        private val ambientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
        }
        private val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (strokeWidthPx * 0.9f).coerceAtLeast(1f)
            strokeCap = Paint.Cap.ROUND
        }
        private val fillPath = Path()
        private val strokePath = Path()
        private var specularStartX = 0f
        private var specularEndX = 0f
        private var specularY = 0f
        private val radii = floatArrayOf(
            cornerRadiusPx, cornerRadiusPx,
            cornerRadiusPx, cornerRadiusPx,
            cornerRadiusPx, cornerRadiusPx,
            cornerRadiusPx, cornerRadiusPx,
        )

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            if (w <= 0f || h <= 0f) return

            fillPath.reset()
            fillPath.addRoundRect(0f, 0f, w, h, radii, Path.Direction.CW)

            val half = strokeWidthPx / 2f
            strokePath.reset()
            strokePath.addRoundRect(half, half, w - half, h - half, radii, Path.Direction.CW)

            if (ambientColor != Color.TRANSPARENT) {
                val radius = (w * 0.62f).coerceAtLeast(1f)
                ambientPaint.shader = RadialGradient(
                    w / 2f,
                    h * 0.28f,
                    radius,
                    intArrayOf(ambientColor, Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                )
            } else {
                ambientPaint.shader = null
            }

            strokePaint.shader = LinearGradient(
                0f,
                0f,
                0f,
                h,
                edgeTopColor,
                edgeBottomColor,
                Shader.TileMode.CLAMP,
            )

            if (specularColor != Color.TRANSPARENT && w > cornerRadiusPx * 2f) {
                val inset = (cornerRadiusPx * 0.85f).coerceAtMost(w * 0.3f)
                specularStartX = inset
                specularEndX = w - inset
                specularY = strokeWidthPx
                specularPaint.shader = LinearGradient(
                    specularStartX,
                    specularY,
                    specularEndX,
                    specularY,
                    intArrayOf(Color.TRANSPARENT, specularColor, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP,
                )
            } else {
                specularPaint.shader = null
            }
        }

        override fun draw(canvas: Canvas) {
            if (bounds.isEmpty) return
            canvas.drawPath(fillPath, fillPaint)
            if (ambientPaint.shader != null) {
                canvas.drawPath(fillPath, ambientPaint)
            }
            canvas.drawPath(strokePath, strokePaint)
            if (specularPaint.shader != null && specularEndX > specularStartX) {
                canvas.drawLine(specularStartX, specularY, specularEndX, specularY, specularPaint)
            }
        }

        override fun setAlpha(alpha: Int) {
            fillPaint.alpha = alpha
            ambientPaint.alpha = alpha
            strokePaint.alpha = alpha
            specularPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            fillPaint.colorFilter = colorFilter
            ambientPaint.colorFilter = colorFilter
            strokePaint.colorFilter = colorFilter
            specularPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
