package com.originisle.android.island

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.originisle.android.R
import com.originisle.android.service.IconCache
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Engine Đảo Động nổi trực tiếp bằng WindowManager + ComposeView cho One UI 5.1+ (không dùng
 * AccessibilityService). Quản lý danh sách thẻ theo id và hiệu chuẩn 4 lớp chống lệch lỗ camera.
 */
object OneUiOverlayIsland {

    private const val TAG = "OneUiOverlayIsland"
    private const val PREFS_NAME = "experimental_prefs"

    private const val KEY_OFFSET_X = "oneui_island_offset_x_px"
    private const val KEY_OFFSET_Y = "oneui_island_offset_y_px"
    private const val KEY_PILL_SCALE = "oneui_island_pill_scale_pct"
    private const val KEY_ISLAND_POS = "island_position"
    private const val KEY_CUSTOM_LIGHT = "cast_custom_light_color"

    private val mainHandler = Handler(Looper.getMainLooper())

    private class OverlayLifecycleOwner :
        LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateController = SavedStateRegistryController.create(this)

        init {
            savedStateController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun onStart() {
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun onDestroy() {
            if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            }
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry
    }

    private data class OverlayActionItem(
        val title: String,
        val icon: Icon?,
        val pendingIntent: PendingIntent?,
    )

    private data class OverlayCard(
        val id: Int,
        val scene: String,
        val title: String,
        val text: String,
        val subtext: String?,
        val sourceApp: String,
        val leftContent: String,
        val rightContent: String,
        val statusChip: String,
        val template: Int,
        val rightTemplate: Int,
        val extra1: String,
        val extra2: String,
        val extra3: String,
        val extra4: String,
        val midText: String?,
        val bgColor: Int,
        val fgColor: Int,
        val lightColor: Int,
        val showProgress: Boolean,
        val progress: Int,
        val progressIndeterminate: Boolean,
        val progressColor: Int,
        val showLeftIcon: Boolean,
        val showRightIcon: Boolean,
        val leftIcon: Icon?,
        val rightIcon: Icon?,
        val largeIcon: Icon?,
        val subImage: Icon?,
        val subClickResp: PendingIntent?,
        val clickResp: PendingIntent?,
        val islandClickExpand: Boolean,
        val waveState: Int,
        val actions: List<OverlayActionItem>,
    )

    private data class OverlayMetricsConfig(
        val screenWidthPx: Int = 1080,
        val cutoutCenterX: Int = 540,
        val cutoutCenterY: Int = 54,
        val cutoutWidthPx: Int = 42,
        val cutoutHeightPx: Int = 42,
        val offsetX: Int = 0,
        val offsetY: Int = 0,
        val pillScalePct: Int = 100,
        val islandPosition: String = "CENTER",
        val customLightColor: String = "NONE",
    )

    private val cards = LinkedHashMap<Int, OverlayCard>()
    private var currentCardState by mutableStateOf<OverlayCard?>(null)
    private var metricsConfigState by mutableStateOf(OverlayMetricsConfig())
    private var isExpandedState by mutableStateOf(false)

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var appContext: Context? = null
    private var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var lastShownCardId: Int? = null

    fun postOrUpdate(context: Context, id: Int, scene: String, intent: Intent) {
        val appCtx = context.applicationContext
        val card = parseCard(appCtx, id, scene, intent)
        runOnMain {
            if (!Settings.canDrawOverlays(appCtx)) {
                hideOverlayLocked()
                return@runOnMain
            }
            cards.remove(id)
            cards[id] = card
            if (lastShownCardId != id) {
                isExpandedState = false
                lastShownCardId = id
            }
            refreshActiveCardLocked()
            ensureOverlayLocked(appCtx)
            updateWindowMetricsAndPositionLocked(appCtx)
        }
    }

    fun cancel(context: Context, id: Int) {
        val appCtx = context.applicationContext
        runOnMain {
            cards.remove(id)
            if (lastShownCardId == id) {
                isExpandedState = false
            }
            refreshActiveCardLocked()
            if (cards.isEmpty()) {
                hideOverlayLocked()
            } else if (Settings.canDrawOverlays(appCtx)) {
                ensureOverlayLocked(appCtx)
                updateWindowMetricsAndPositionLocked(appCtx)
            }
        }
    }

    fun cancelAll(context: Context) {
        runOnMain {
            cards.clear()
            isExpandedState = false
            lastShownCardId = null
            currentCardState = null
            hideOverlayLocked()
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun refreshActiveCardLocked() {
        val priorityCard = cards.values.lastOrNull {
            it.scene == "INCALLING" || it.scene == "VOIPCALL"
        } ?: cards.values.lastOrNull()
        currentCardState = priorityCard
        lastShownCardId = priorityCard?.id
    }

    private fun ensurePrefsListenerLocked(context: Context) {
        if (prefsListener != null) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_OFFSET_X ||
                key == KEY_OFFSET_Y ||
                key == KEY_PILL_SCALE ||
                key == KEY_ISLAND_POS ||
                key == KEY_CUSTOM_LIGHT
            ) {
                runOnMain {
                    val ctx = appContext ?: return@runOnMain
                    if (cards.isNotEmpty() && Settings.canDrawOverlays(ctx)) {
                        updateWindowMetricsAndPositionLocked(ctx)
                    }
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        prefsListener = listener
    }

    private fun ensureOverlayLocked(context: Context) {
        appContext = context
        ensurePrefsListenerLocked(context)

        val existing = overlayView
        if (existing != null) {
            if (existing.visibility != View.VISIBLE) {
                existing.visibility = View.VISIBLE
            }
            return
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val card = currentCardState
                val config = metricsConfigState
                if (card != null) {
                    OneUiIslandRoot(
                        context = context,
                        card = card,
                        config = config,
                        isExpanded = isExpandedState,
                        onToggleExpand = { isExpandedState = !isExpandedState },
                        onCollapse = { isExpandedState = false },
                    )
                }
            }
            addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                v.post { calibratePositionLocked(context) }
            }
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        runCatching {
            wm.addView(composeView, lp)
            overlayView = composeView
            layoutParams = lp
            owner.onStart()
        }.onFailure { err ->
            Log.e(TAG, "Failed to attach OneUiOverlayIsland", err)
            owner.onDestroy()
            lifecycleOwner = null
            overlayView = null
            layoutParams = null
        }
    }

    private fun hideOverlayLocked() {
        overlayView?.visibility = View.GONE
    }

    private fun updateWindowMetricsAndPositionLocked(context: Context) {
        val wm = windowManager ?: (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            ?: return
        windowManager = wm

        val metrics = wm.currentWindowMetrics
        val screenBounds = metrics.bounds
        val cutout = metrics.windowInsets.displayCutout
        val topRect = cutout?.boundingRectTop?.takeIf { !it.isEmpty }
            ?: cutout?.boundingRects?.firstOrNull {
                !it.isEmpty && it.top <= screenBounds.top + dpToPx(context, 48f)
            }

        val cutoutCenterX = topRect?.centerX() ?: (screenBounds.width() / 2)
        val cutoutCenterY = topRect?.centerY() ?: dpToPx(context, 18f)
        val cutoutWidthPx = (topRect?.width() ?: dpToPx(context, 14f))
            .coerceIn(dpToPx(context, 10f), dpToPx(context, 90f))
        val cutoutHeightPx = (topRect?.height() ?: dpToPx(context, 14f))
            .coerceIn(dpToPx(context, 10f), dpToPx(context, 44f))

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val offsetX = prefs.getInt(KEY_OFFSET_X, 0)
        val offsetY = prefs.getInt(KEY_OFFSET_Y, 0)
        val pillScalePct = prefs.getInt(KEY_PILL_SCALE, 100).coerceIn(60, 160)
        val islandPosition = prefs.getString(KEY_ISLAND_POS, "CENTER")?.trim()
            ?.ifBlank { "CENTER" } ?: "CENTER"
        val customLightColor = prefs.getString(KEY_CUSTOM_LIGHT, "NONE")?.trim()
            ?.ifBlank { "NONE" } ?: "NONE"

        val config = OverlayMetricsConfig(
            screenWidthPx = screenBounds.width(),
            cutoutCenterX = cutoutCenterX,
            cutoutCenterY = cutoutCenterY,
            cutoutWidthPx = cutoutWidthPx,
            cutoutHeightPx = cutoutHeightPx,
            offsetX = offsetX,
            offsetY = offsetY,
            pillScalePct = pillScalePct,
            islandPosition = islandPosition,
            customLightColor = customLightColor,
        )
        metricsConfigState = config

        val view = overlayView ?: return
        val lp = layoutParams ?: return
        if (view.visibility != View.VISIBLE) {
            view.visibility = View.VISIBLE
        }

        val desiredTop = computeDesiredTopPx(context, config)
        lp.y = desiredTop
        runCatching { wm.updateViewLayout(view, lp) }
        view.post { calibratePositionLocked(context) }
    }

    private fun calibratePositionLocked(context: Context) {
        val view = overlayView ?: return
        val lp = layoutParams ?: return
        val wm = windowManager ?: return
        if (!view.isAttachedToWindow || view.visibility != View.VISIBLE) return
        val viewWidth = view.width
        if (viewWidth <= 0) return

        val config = metricsConfigState
        val desiredTop = computeDesiredTopPx(context, config)
        val desiredX = when (config.islandPosition.uppercase()) {
            "LEFT" -> dpToPx(context, 12f) + config.offsetX
            "RIGHT" -> (config.screenWidthPx - viewWidth - dpToPx(context, 12f) + config.offsetX)
                .coerceAtLeast(0)
            else -> (config.cutoutCenterX + config.offsetX - (viewWidth / 2))
                .coerceIn(0, (config.screenWidthPx - viewWidth).coerceAtLeast(0))
        }

        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val deltaY = loc[1] - desiredTop

        var changed = false
        if (abs(deltaY) in 1..200) {
            lp.y -= deltaY
            changed = true
        }
        if (lp.x != desiredX) {
            lp.x = desiredX
            changed = true
        }
        if (changed) {
            runCatching { wm.updateViewLayout(view, lp) }
        }
    }

    private fun computeDesiredTopPx(context: Context, config: OverlayMetricsConfig): Int {
        val scale = config.pillScalePct / 100f
        val basePillHeightPx = max(config.cutoutHeightPx + dpToPx(context, 10f), dpToPx(context, 34f))
        val scaledPillHeightPx = (basePillHeightPx * scale).roundToInt()
        return (config.cutoutCenterY + config.offsetY) - (scaledPillHeightPx / 2)
    }

    private fun dpToPx(context: Context, dp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics,
        ).roundToInt()

    private fun parseCard(context: Context, id: Int, scene: String, intent: Intent): OverlayCard {
        val title = intent.getStringExtra("title").orEmpty()
        val text = intent.getStringExtra("text").orEmpty()
        val subtext = intent.getStringExtra("subtext")
        val sourceApp = intent.getStringExtra("source_app").orEmpty()
        val leftContent = intent.getStringExtra("oi_left_content").orEmpty()
        val rightContent = intent.getStringExtra("oi_right_content").orEmpty()
        val statusChip = intent.getStringExtra("status_chip_text").orEmpty()
        val template = intent.getIntExtra("oi_template", 0)
        val rightTemplate = intent.getIntExtra("oi_right_template", 0)

        val extra1 = intent.getStringExtra("oi_extra1").orEmpty()
        val extra2 = intent.getStringExtra("oi_extra2").orEmpty()
        val extra3 = intent.getStringExtra("oi_extra3").orEmpty()
        val extra4 = intent.getStringExtra("oi_extra4").orEmpty()
        val midText = intent.getStringExtra("oi_mid_text")

        val bgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_bg_color"), 0)
        val fgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_fg_color"), 0)
        val lightColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_light_color"), 0)
        val progressColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_progress_color"), 0)

        val showProgress = intent.getBooleanExtra("show_progress", false)
        val progressMax = intent.getIntExtra("progress_max", 0)
        val rawProgress = intent.getIntExtra("progress", 0)
        val progress = when {
            intent.hasExtra("oi_progress") -> intent.getIntExtra("oi_progress", 0)
            showProgress && progressMax > 0 -> rawProgress * 100 / progressMax
            else -> rawProgress
        }.coerceIn(0, 100)
        val progressIndeterminate = intent.getBooleanExtra("progress_indeterminate", false)

        val showLeftIcon = intent.getBooleanExtra("oi_show_left_icon", true)
        val showRightIcon = intent.getBooleanExtra("oi_show_right_icon", true)

        val iconRes = intent.getIntExtra("icon_res", R.mipmap.ic_launcher_round)
        val baseIcon = IconCache.activeSmallIcons[id]
            ?: IconCache.safeResourceIcon(context, iconRes)
        val leftIcon = intent.getParcelableExtra("oi_left_icon", Icon::class.java) ?: baseIcon
        val rightIcon = intent.getParcelableExtra("oi_right_icon", Icon::class.java) ?: baseIcon
        val largeIcon = intent.getParcelableExtra("oi_large_icon", Icon::class.java)
            ?: IconCache.activeLargeIcons[id]
            ?: IconCache.activeLargeBitmaps[id]?.let { Icon.createWithBitmap(it) }
        val subImage = intent.getParcelableExtra("oi_sub_image", Icon::class.java)
        val subClickResp = intent.getParcelableExtra("oi_sub_click_resp", PendingIntent::class.java)
        val clickResp = intent.getParcelableExtra("oi_capsule_click_resp", PendingIntent::class.java)
            ?: intent.getParcelableExtra("source_content_intent", PendingIntent::class.java)
            ?: intent.getParcelableExtra("click_resp", PendingIntent::class.java)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tapExpandCard = prefs.getBoolean("cast_tap_expand_card", true)
        val rawExpand = intent.getBooleanExtra("oi_island_click_expand", false)
        val isMandatory = id in setOf(35001, 35004, 35011, 35014) ||
            scene in setOf("INCALLING", "VOIPCALL")
        val islandClickExpand = when {
            isMandatory -> rawExpand
            !tapExpandCard && clickResp != null -> false
            else -> tapExpandCard || rawExpand
        }

        val waveState = intent.getIntExtra("oi_wave_state", 1)

        val fromNotifActions = intent.getParcelableArrayListExtra("actions", Notification.Action::class.java)
            ?.take(3)
            ?.map { a ->
                OverlayActionItem(
                    title = a.title?.toString().orEmpty(),
                    icon = a.getIcon(),
                    pendingIntent = a.actionIntent,
                )
            }
        val actions = if (!fromNotifActions.isNullOrEmpty()) {
            fromNotifActions
        } else {
            val btnPis = intent.getParcelableArrayListExtra("oi_button_actions", PendingIntent::class.java).orEmpty()
            val btnIcons = intent.getParcelableArrayListExtra("oi_button_icons", Icon::class.java).orEmpty()
            val btnTitles = intent.getStringArrayListExtra("oi_button_titles").orEmpty()
            val count = max(btnPis.size, btnTitles.size).coerceAtMost(3)
            (0 until count).map { idx ->
                OverlayActionItem(
                    title = btnTitles.getOrNull(idx).orEmpty(),
                    icon = btnIcons.getOrNull(idx),
                    pendingIntent = btnPis.getOrNull(idx),
                )
            }
        }

        return OverlayCard(
            id = id,
            scene = scene,
            title = title,
            text = text,
            subtext = subtext,
            sourceApp = sourceApp,
            leftContent = leftContent,
            rightContent = rightContent.ifBlank { statusChip },
            statusChip = statusChip,
            template = template,
            rightTemplate = rightTemplate,
            extra1 = extra1,
            extra2 = extra2,
            extra3 = extra3,
            extra4 = extra4,
            midText = midText,
            bgColor = bgColor,
            fgColor = fgColor,
            lightColor = lightColor,
            showProgress = showProgress,
            progress = progress,
            progressIndeterminate = progressIndeterminate,
            progressColor = progressColor,
            showLeftIcon = showLeftIcon,
            showRightIcon = showRightIcon,
            leftIcon = leftIcon,
            rightIcon = rightIcon,
            largeIcon = largeIcon,
            subImage = subImage,
            subClickResp = subClickResp,
            clickResp = clickResp,
            islandClickExpand = islandClickExpand,
            waveState = waveState,
            actions = actions,
        )
    }

    @Composable
    private fun OneUiIslandRoot(
        context: Context,
        card: OverlayCard,
        config: OverlayMetricsConfig,
        isExpanded: Boolean,
        onToggleExpand: () -> Unit,
        onCollapse: () -> Unit,
    ) {
        val density = LocalDensity.current
        val scale = config.pillScalePct / 100f
        val cutoutHeightDp = with(density) { config.cutoutHeightPx.toDp() }
        val cutoutWidthDp = with(density) { config.cutoutWidthPx.toDp() }
        val screenWidthDp = with(density) { config.screenWidthPx.toDp() }
        val pillHeightDp = maxOf(cutoutHeightDp + 10.dp, 34.dp) * scale

        val glowColor = remember(config.customLightColor, card.lightColor) {
            resolveGlowColor(config.customLightColor, card.lightColor)
        }

        Box(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 380f,
                    ),
                ),
        ) {
            AnimatedContent(
                targetState = isExpanded,
                label = "OneUiIslandExpandTransition",
            ) { expanded ->
                if (!expanded) {
                    CollapsedPill(
                        context = context,
                        card = card,
                        isCenter = config.islandPosition.equals("CENTER", ignoreCase = true),
                        pillHeightDp = pillHeightDp,
                        cutoutWidthDp = cutoutWidthDp,
                        scale = scale,
                        glowColor = glowColor,
                        onClick = {
                            if (card.islandClickExpand) {
                                onToggleExpand()
                            } else {
                                sendPendingIntentSafe(card.clickResp)
                            }
                        },
                    )
                } else {
                    ExpandedSquircleCard(
                        context = context,
                        card = card,
                        screenWidthDp = screenWidthDp,
                        glowColor = glowColor,
                        onCollapse = onCollapse,
                    )
                }
            }
        }
    }

    @Composable
    private fun CollapsedPill(
        context: Context,
        card: OverlayCard,
        isCenter: Boolean,
        pillHeightDp: Dp,
        cutoutWidthDp: Dp,
        scale: Float,
        glowColor: Color,
        onClick: () -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val horizontalPad = (10.dp * scale).coerceAtLeast(6.dp)
        val iconSize = (18.dp * scale).coerceAtLeast(14.dp)

        Row(
            modifier = Modifier
                .height(pillHeightDp)
                .widthIn(min = cutoutWidthDp + 52.dp, max = 340.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, glowColor, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = horizontalPad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Khoang Trái
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                if (card.showLeftIcon && card.leftIcon != null) {
                    IconBitmapView(
                        context = context,
                        icon = card.leftIcon,
                        size = iconSize,
                        rounded = true,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                val leftText = card.leftContent.ifBlank { card.title }
                if (leftText.isNotBlank()) {
                    Text(
                        text = leftText,
                        color = Color.White,
                        fontSize = (12.sp * scale),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 104.dp),
                    )
                }
            }

            if (isCenter) {
                Spacer(Modifier.width(cutoutWidthDp + 12.dp))
            } else {
                Spacer(Modifier.width(10.dp))
            }

            // Khoang Phải
            RightPillCompartment(
                context = context,
                card = card,
                iconSize = iconSize,
                scale = scale,
            )
        }
    }

    @Composable
    private fun RightPillCompartment(
        context: Context,
        card: OverlayCard,
        iconSize: Dp,
        scale: Float,
    ) {
        val fgColor = if ((card.fgColor ushr 24) != 0) Color(card.fgColor) else Color.White
        val bgColor = if ((card.bgColor ushr 24) != 0) Color(card.bgColor) else Color(0xFF2B2F3A)

        when (card.rightTemplate) {
            OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE -> {
                MusicWaveBars(
                    active = card.waveState != 0,
                    color = fgColor,
                    scale = scale,
                )
            }
            OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pColor = if ((card.progressColor ushr 24) != 0) Color(card.progressColor) else Color(0xFF4ADE80)
                    Box(
                        modifier = Modifier.size(iconSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(iconSize)) {
                            val strokeWidth = 2.dp.toPx()
                            drawArc(
                                color = Color(0x33FFFFFF),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = pColor,
                                startAngle = -90f,
                                sweepAngle = (card.progress.coerceIn(0, 100) / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${card.progress}%",
                        color = fgColor,
                        fontSize = (11.sp * scale),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
            OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON,
            OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_ICON_TEXT,
            -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (card.rightTemplate == OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_ICON_TEXT &&
                        card.showRightIcon && card.rightIcon != null
                    ) {
                        IconBitmapView(context = context, icon = card.rightIcon, size = iconSize, rounded = true)
                        if (card.rightContent.isNotBlank()) Spacer(Modifier.width(4.dp))
                    }
                    if (card.rightContent.isNotBlank()) {
                        Text(
                            text = card.rightContent,
                            color = fgColor,
                            fontSize = (12.sp * scale),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 90.dp),
                        )
                    }
                    if (card.rightTemplate == OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON &&
                        card.showRightIcon && card.rightIcon != null
                    ) {
                        if (card.rightContent.isNotBlank()) Spacer(Modifier.width(4.dp))
                        IconBitmapView(context = context, icon = card.rightIcon, size = iconSize, rounded = true)
                    }
                }
            }
            OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT -> {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bgColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = card.rightContent.ifBlank { card.statusChip },
                        color = fgColor,
                        fontSize = (11.sp * scale),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 84.dp),
                    )
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (card.rightContent.isNotBlank()) {
                        Text(
                            text = card.rightContent,
                            color = fgColor,
                            fontSize = (12.sp * scale),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 88.dp),
                        )
                    } else if (card.showRightIcon && card.rightIcon != null) {
                        IconBitmapView(context = context, icon = card.rightIcon, size = iconSize, rounded = true)
                    }
                }
            }
        }
    }

    @Composable
    private fun MusicWaveBars(active: Boolean, color: Color, scale: Float) {
        val infiniteTransition = rememberInfiniteTransition(label = "OneUiWave")
        val anim1 by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
            label = "b1",
        )
        val anim2 by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
            label = "b2",
        )
        val anim3 by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(380), RepeatMode.Reverse),
            label = "b3",
        )
        val anim4 by infiniteTransition.animateFloat(
            initialValue = 0.75f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(tween(460), RepeatMode.Reverse),
            label = "b4",
        )
        val heights = if (active) listOf(anim1, anim2, anim3, anim4) else listOf(0.35f, 0.35f, 0.35f, 0.35f)
        val maxH = (14.dp * scale).coerceAtLeast(10.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(maxH),
        ) {
            heights.forEach { ratio ->
                Box(
                    modifier = Modifier
                        .width((2.5.dp * scale).coerceAtLeast(2.dp))
                        .height(maxH * ratio)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }

    @Composable
    private fun ExpandedSquircleCard(
        context: Context,
        card: OverlayCard,
        screenWidthDp: Dp,
        glowColor: Color,
        onCollapse: () -> Unit,
    ) {
        val cardWidth = min(screenWidthDp.value - 24f, 380f).dp
        val shape = RoundedCornerShape(28.dp)
        val borderColor = if (glowColor != Color(0x2EFFFFFF)) glowColor else Color(0x2EFFFFFF)

        Column(
            modifier = Modifier
                .width(cardWidth)
                .clip(shape)
                .background(Color(0xF216181E))
                .border(1.dp, borderColor, shape)
                .clickable {
                    sendPendingIntentSafe(card.clickResp)
                    onCollapse()
                }
                .padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val headerIcon = card.largeIcon ?: card.leftIcon
                if (headerIcon != null) {
                    IconBitmapView(
                        context = context,
                        icon = headerIcon,
                        size = 44.dp,
                        rounded = true,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    val sub = card.subtext?.takeIf { it.isNotBlank() } ?: card.sourceApp
                    if (sub.isNotBlank()) {
                        Text(
                            text = sub,
                            color = Color(0x99FFFFFF),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (card.title.isNotBlank()) {
                        Text(
                            text = card.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (card.text.isNotBlank()) {
                        Text(
                            text = card.text,
                            color = Color(0xCCFFFFFF),
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x1FFFFFFF))
                        .clickable { onCollapse() },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        val path = Path().apply {
                            moveTo(0f, size.height * 0.7f)
                            lineTo(size.width * 0.5f, size.height * 0.25f)
                            lineTo(size.width, size.height * 0.7f)
                        }
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            }

            // oi_sub_image
            if (card.subImage != null) {
                val subBitmap = remember(card.subImage) { card.subImage.toImageBitmap(context) }
                if (subBitmap != null) {
                    Spacer(Modifier.height(12.dp))
                    Image(
                        bitmap = subBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                sendPendingIntentSafe(card.subClickResp ?: card.clickResp)
                                onCollapse()
                            },
                    )
                }
            }

            // 3 cột đối xứng cho TEMPLATE_TEXT_SYMMETRY (template == 3)
            if (card.template == OriginIslandConstants.TEMPLATE_TEXT_SYMMETRY) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        if (card.extra1.isNotBlank()) {
                            Text(
                                text = card.extra1,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (card.extra2.isNotBlank()) {
                            Text(
                                text = card.extra2,
                                color = Color(0x99FFFFFF),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = card.midText.orEmpty(),
                        color = Color(0xCCFFFFFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                    ) {
                        if (card.extra3.isNotBlank()) {
                            Text(
                                text = card.extra3,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (card.extra4.isNotBlank()) {
                            Text(
                                text = card.extra4,
                                color = Color(0x99FFFFFF),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Progress bar
            if (card.template == OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL || card.showProgress) {
                Spacer(Modifier.height(12.dp))
                val pColor = if ((card.progressColor ushr 24) != 0) Color(card.progressColor) else Color(0xFF4ADE80)
                if (card.progressIndeterminate) {
                    LinearProgressIndicator(
                        color = pColor,
                        trackColor = Color(0x26FFFFFF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { card.progress / 100f },
                        color = pColor,
                        trackColor = Color(0x26FFFFFF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                }
            }

            // Tối đa 3 nút thao tác
            if (card.actions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    card.actions.take(3).forEach { action ->
                        Button(
                            onClick = { sendPendingIntentSafe(action.pendingIntent) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x24FFFFFF),
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                        ) {
                            if (action.icon != null) {
                                IconBitmapView(
                                    context = context,
                                    icon = action.icon,
                                    size = 16.dp,
                                    rounded = false,
                                )
                                if (action.title.isNotBlank()) Spacer(Modifier.width(4.dp))
                            }
                            if (action.title.isNotBlank()) {
                                Text(
                                    text = action.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun IconBitmapView(
        context: Context,
        icon: Icon,
        size: Dp,
        rounded: Boolean,
    ) {
        val bitmap = remember(icon) { icon.toImageBitmap(context) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .then(if (rounded) Modifier.clip(CircleShape) else Modifier),
            )
        }
    }

    private fun Icon.toImageBitmap(context: Context): ImageBitmap? = runCatching {
        val drawable = loadDrawable(context) ?: return@runCatching null
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return@runCatching drawable.bitmap.asImageBitmap()
        }
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    }.getOrNull()

    private fun resolveGlowColor(customLightPref: String, cardLightColor: Int): Color {
        if (!customLightPref.equals("NONE", ignoreCase = true) && customLightPref.startsWith("#")) {
            val parsed = OriginIslandBuilder.parseColor(customLightPref, 0)
            if ((parsed ushr 24) != 0) return Color(parsed)
        }
        if ((cardLightColor ushr 24) != 0) return Color(cardLightColor)
        return Color(0x2EFFFFFF)
    }

    private fun sendPendingIntentSafe(pi: PendingIntent?) {
        if (pi == null) return
        runCatching { pi.send() }
            .onFailure { Log.w(TAG, "Failed to send PendingIntent: ${it.message}") }
    }
}
