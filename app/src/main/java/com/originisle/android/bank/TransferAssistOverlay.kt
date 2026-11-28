package com.originisle.android.bank

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.originisle.android.ui.PREFS_NAME
import com.originisle.android.ui.PREF_DEFAULT_BANK_PKG
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Floating OLED black OriginOS-style transfer assistant overlay rendered via [WindowManager].
 * Displays parsed bank transfer details (bank name, account number, amount, note) and provides
 * 1-tap copy, QR image sharing, and quick bank app launch actions while the user interacts
 * with their banking app underneath.
 */
object TransferAssistOverlay {

    private const val TAG = "TransferAssistOverlay"
    private const val ANIM_DURATION_MS = 200L

    private const val CARD_BG_COLOR = 0xFF000000.toInt()
    private const val CARD_EDGE_LIGHT_TOP = 0x38FFFFFF
    private const val CARD_EDGE_LIGHT_BOTTOM = 0x0DFFFFFF
    private const val PILL_BG_COLOR = 0x1FFFFFFF
    private const val PILL_BORDER_COLOR = 0x26FFFFFF
    private const val CLOSE_BTN_BG_COLOR = 0x24FFFFFF
    private const val TEXT_PRIMARY = Color.WHITE
    private const val TEXT_SECONDARY = 0xB3FFFFFF.toInt()
    private const val ACCENT_GREEN = 0xFF00D084.toInt()
    private const val RIPPLE_COLOR = 0x33FFFFFF

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var rootView: FrameLayout? = null

    @Volatile
    private var cardView: LinearLayout? = null

    @Volatile
    private var windowManager: WindowManager? = null

    @Volatile
    private var isDismissing = false

    fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isShowing(): Boolean = rootView != null && !isDismissing

    /**
     * Copies [ParsedBankTransfer.accountNumber] immediately and displays the floating overlay
     * at the top of the screen if overlay permission is granted.
     */
    fun show(
        context: Context,
        transfer: ParsedBankTransfer,
        qrImageUri: Uri? = null,
        defaultBankPkg: String? = null,
    ): Boolean {
        val appContext = context.applicationContext
        if (transfer.accountNumber.isNotBlank()) {
            copyToClipboard(appContext, "STK", transfer.accountNumber)
        }
        if (!canShow(appContext)) return false

        val resolvedBankPkg = resolveBankPackage(appContext, defaultBankPkg)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return showOnMainThread(appContext, transfer, qrImageUri, resolvedBankPkg)
        }
        mainHandler.post {
            showOnMainThread(appContext, transfer, qrImageUri, resolvedBankPkg)
        }
        return true
    }

    fun dismiss() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dismissOnMainThread()
        } else {
            mainHandler.post { dismissOnMainThread() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOnMainThread(
        appContext: Context,
        transfer: ParsedBankTransfer,
        qrImageUri: Uri?,
        resolvedBankPkg: String?,
    ): Boolean {
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

        val root = FrameLayout(appContext)

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
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))

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
            val pureBlackBase = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = innerCorner
                setColor(CARD_BG_COLOR)
            }
            background = LayerDrawable(arrayOf(edgeLightBorder, pureBlackBase)).apply {
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
                        if (v.translationY < -dp(36f)) {
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

        // Top row: Bank name + Close button (×)
        val headerRow = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val infoColumn = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val bankTitleTv = TextView(appContext).apply {
            text = transfer.bankName.ifBlank { "Chuyển khoản ngân hàng" }
            setTextColor(ACCENT_GREEN)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        infoColumn.addView(bankTitleTv)

        val accountTv = TextView(appContext).apply {
            text = "STK: ${transfer.accountNumber}"
            setTextColor(TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            fontFeatureSettings = "tnum"
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(2f), 0, 0)
        }
        infoColumn.addView(accountTv)

        if (transfer.amount.isNotBlank()) {
            val formattedAmount = formatVndAmount(transfer.amount)
            val amountTv = TextView(appContext).apply {
                text = "Số tiền: $formattedAmount"
                setTextColor(TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                fontFeatureSettings = "tnum"
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(3f), 0, 0)
            }
            infoColumn.addView(amountTv)
        }

        if (transfer.note.isNotBlank()) {
            val noteTv = TextView(appContext).apply {
                text = "Nội dung: ${transfer.note}"
                setTextColor(TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(2f), 0, 0)
            }
            infoColumn.addView(noteTv)
        }

        headerRow.addView(infoColumn)

        val closeBtnSize = dp(32f)
        val closeBtn = TextView(appContext).apply {
            text = "×"
            gravity = Gravity.CENTER
            setTextColor(TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            contentDescription = "Đóng"
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(CLOSE_BTN_BG_COLOR)
            }
            background = RippleDrawable(ColorStateList.valueOf(RIPPLE_COLOR), bg, null)
            layoutParams = LinearLayout.LayoutParams(closeBtnSize, closeBtnSize).apply {
                marginStart = dp(10f)
                gravity = Gravity.TOP
            }
            setOnClickListener { dismiss() }
        }
        headerRow.addView(closeBtn)
        card.addView(headerRow)

        // Action pills row
        val actionScroll = HorizontalScrollView(appContext).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10f)
            }
        }

        val actionRow = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        fun createActionPill(label: String, onClick: () -> Unit): TextView {
            return TextView(appContext).apply {
                text = label
                setTextColor(TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(dp(12f), dp(7f), dp(12f), dp(7f))

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(999f).toFloat()
                    setColor(PILL_BG_COLOR)
                    setStroke(dp(1f).coerceAtLeast(1), PILL_BORDER_COLOR)
                }
                background = RippleDrawable(ColorStateList.valueOf(RIPPLE_COLOR), bg, null)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginEnd = dp(8f)
                }

                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate().cancel()
                            v.animate()
                                .scaleX(0.94f)
                                .scaleY(0.94f)
                                .alpha(0.85f)
                                .setDuration(80L)
                                .setInterpolator(DecelerateInterpolator())
                                .start()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate().cancel()
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .alpha(1f)
                                .setDuration(160L)
                                .setInterpolator(OvershootInterpolator(1.5f))
                                .start()
                        }
                    }
                    false
                }
                setOnClickListener { onClick() }
            }
        }

        actionRow.addView(
            createActionPill("Copy STK") {
                copyToClipboard(appContext, "STK", transfer.accountNumber)
                Toast.makeText(
                    appContext,
                    "Đã sao chép STK: ${transfer.accountNumber}",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )

        if (transfer.amount.isNotBlank()) {
            val cleanDigits = transfer.amount.filter { it.isDigit() }.ifBlank { transfer.amount }
            val formatted = formatVndAmount(transfer.amount)
            actionRow.addView(
                createActionPill("Copy Tiền") {
                    copyToClipboard(appContext, "Số tiền", cleanDigits)
                    Toast.makeText(
                        appContext,
                        "Đã sao chép số tiền: $formatted",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            )
        }

        if (transfer.note.isNotBlank()) {
            actionRow.addView(
                createActionPill("Copy Nội dung") {
                    copyToClipboard(appContext, "Nội dung", transfer.note)
                    Toast.makeText(
                        appContext,
                        "Đã sao chép nội dung",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            )
        }

        if (qrImageUri != null) {
            actionRow.addView(
                createActionPill("Chia sẻ ảnh QR") {
                    shareQrImage(appContext, qrImageUri, resolvedBankPkg)
                },
            )
        }

        if (!resolvedBankPkg.isNullOrBlank()) {
            actionRow.addView(
                createActionPill("Mở app NH") {
                    launchBankApp(appContext, resolvedBankPkg)
                },
            )
        }

        actionScroll.addView(actionRow)
        card.addView(actionScroll)

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

            card.alpha = 0f
            card.scaleX = 0.94f
            card.scaleY = 0.94f
            card.translationY = -dp(10f).toFloat()
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(ANIM_DURATION_MS)
                .setInterpolator(DecelerateInterpolator())
                .start()
            true
        }.onFailure {
            Log.e(TAG, "Failed to show TransferAssistOverlay", it)
            removeWindowImmediate()
        }.getOrDefault(false)
    }

    private fun dismissOnMainThread() {
        val card = cardView
        if (rootView == null || isDismissing) return
        if (card == null) {
            removeWindowImmediate()
            return
        }
        isDismissing = true
        val density = card.resources.displayMetrics.density
        card.animate().cancel()
        card.animate()
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .translationY(-10f * density)
            .setDuration(150L)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { removeWindowImmediate() }
            .start()
    }

    private fun removeWindowImmediate() {
        val root = rootView
        val wm = windowManager
        if (root != null && wm != null) {
            runCatching { wm.removeViewImmediate(root) }
        }
        rootView = null
        cardView = null
        windowManager = null
        isDismissing = false
    }

    private fun resolveBankPackage(appContext: Context, explicitPkg: String?): String? {
        if (!explicitPkg.isNullOrBlank()) return explicitPkg
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_DEFAULT_BANK_PKG, null)
        if (!saved.isNullOrBlank() && appContext.packageManager.getLaunchIntentForPackage(saved) != null) {
            return saved
        }
        return null
    }

    private fun shareQrImage(appContext: Context, qrImageUri: Uri, targetBankPkg: String?) {
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, qrImageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!targetBankPkg.isNullOrBlank()) {
            val directIntent = Intent(baseIntent).setPackage(targetBankPkg)
            val started = runCatching {
                appContext.startActivity(directIntent)
                true
            }.getOrDefault(false)
            if (started) return
        }
        val chooser = Intent.createChooser(baseIntent, "Chia sẻ ảnh QR").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            appContext.startActivity(chooser)
        }.onFailure {
            Toast.makeText(appContext, "Không thể chia sẻ ảnh QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchBankApp(appContext: Context, pkg: String) {
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent == null) {
            Toast.makeText(appContext, "Không tìm thấy ứng dụng ngân hàng", Toast.LENGTH_SHORT).show()
            return
        }
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
        )
        try {
            appContext.startActivity(launchIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(appContext, "Không thể mở ứng dụng ngân hàng", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    internal fun formatVndAmount(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        val numeric = digits.toLongOrNull() ?: return raw.trim()
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("vi-VN")).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        return "${formatter.format(numeric)}đ"
    }
}
