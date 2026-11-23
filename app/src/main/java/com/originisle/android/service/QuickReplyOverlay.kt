package com.originisle.android.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.originisle.android.island.PlaygroundService

data class QuickReplySession(
    val cardId: Int,
    val scene: String,
    val sourcePkg: String,
    val sbnKey: String?,
    val sender: String,
    val previewText: String,
    val replyAction: Notification.Action,
    val remoteInput: RemoteInput,
)

/**
 * Cửa sổ nổi dark Material 3 cho phép trả lời nhanh tin nhắn trực tiếp từ Đảo
 * thông qua [RemoteInput] của thông báo gốc.
 */
object QuickReplyOverlay {

    private const val TAG = "QuickReplyOverlay"
    private const val CARD_BG_COLOR = 0xF21B1D22.toInt()
    private const val CARD_STROKE_COLOR = 0x2EFFFFFF
    private const val INPUT_BG_COLOR = 0x1AFFFFFF
    private const val INPUT_STROKE_COLOR = 0x26FFFFFF
    private const val SEND_BTN_COLOR = 0xFF3B82F6.toInt()
    private const val CLOSE_BTN_COLOR = 0x26FFFFFF
    private const val TEXT_PRIMARY = Color.WHITE
    private const val TEXT_SECONDARY = 0xB3FFFFFF.toInt()
    private const val TEXT_HINT = 0x73FFFFFF

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var currentView: View? = null

    @Volatile
    private var currentWindowManager: WindowManager? = null

    fun canShowOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun show(context: Context, session: QuickReplySession): Boolean {
        if (!canShowOverlay(context)) return false
        val appContext = context.applicationContext

        if (Looper.myLooper() == Looper.getMainLooper()) {
            return showOnMainThread(appContext, session)
        }
        mainHandler.post { showOnMainThread(appContext, session) }
        return true
    }

    fun dismiss() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dismissOnMainThread()
        } else {
            mainHandler.post { dismissOnMainThread() }
        }
    }

    private fun showOnMainThread(appContext: Context, session: QuickReplySession): Boolean {
        dismissOnMainThread()

        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return false
        val density = appContext.resources.displayMetrics.density
        fun dp(value: Float): Int = (value * density).toInt()

        val root = object : FrameLayout(appContext) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dismiss()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    dismiss()
                    return true
                }
                return super.onTouchEvent(event)
            }
        }

        val card = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(16f), dp(18f), dp(16f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24f).toFloat()
                setColor(CARD_BG_COLOR)
                setStroke(dp(1f).coerceAtLeast(1), CARD_STROKE_COLOR)
            }
            elevation = dp(12f).toFloat()
        }

        // Header: tên người gửi + nút đóng X
        val header = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val senderView = TextView(appContext).apply {
            text = session.sender.ifBlank { "Tin nhắn mới" }
            setTextColor(TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(12f)
            }
        }

        val closeBtn = TextView(appContext).apply {
            text = "X"
            setTextColor(TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(CLOSE_BTN_COLOR)
            }
            layoutParams = LinearLayout.LayoutParams(dp(30f), dp(30f))
            contentDescription = "Đóng"
            setOnClickListener { dismiss() }
        }

        header.addView(senderView)
        header.addView(closeBtn)
        card.addView(header)

        // Dòng preview nội dung tin nhắn
        if (session.previewText.isNotBlank()) {
            val previewView = TextView(appContext).apply {
                text = session.previewText
                setTextColor(TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(6f)
                }
            }
            card.addView(previewView)
        }

        // Hàng nhập liệu + nút Gửi
        val inputRow = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(12f)
            }
        }

        val editText = EditText(appContext).apply {
            hint = session.remoteInput.label?.toString()?.takeIf { it.isNotBlank() } ?: "Nhập trả lời..."
            setTextColor(TEXT_PRIMARY)
            setHintTextColor(TEXT_HINT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 3
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16f).toFloat()
                setColor(INPUT_BG_COLOR)
                setStroke(dp(1f).coerceAtLeast(1), INPUT_STROKE_COLOR)
            }
            isFocusable = true
            isFocusableInTouchMode = true
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(10f)
            }
        }

        fun submitReply() {
            val replyText = editText.text?.toString()?.trim().orEmpty()
            if (replyText.isEmpty()) {
                editText.requestFocus()
                return
            }
            sendReply(appContext, session, replyText)
            dismiss()
        }

        editText.setOnEditorActionListener { _, actionId, event ->
            val isEnterSend = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_SEND || isEnterSend) {
                submitReply()
                true
            } else {
                false
            }
        }

        val sendBtn = TextView(appContext).apply {
            text = "Gửi"
            setTextColor(TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(18f), dp(10f), dp(18f), dp(10f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18f).toFloat()
                setColor(SEND_BTN_COLOR)
            }
            setOnClickListener { submitReply() }
        }

        inputRow.addView(editText)
        inputRow.addView(sendBtn)
        card.addView(inputRow)

        val cardParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            setMargins(dp(16f), 0, dp(16f), 0)
        }
        root.addView(card, cardParams)

        val windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(48f)
            dimAmount = 0.45f
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        return runCatching {
            wm.addView(root, windowParams)
            currentView = root
            currentWindowManager = wm
            editText.post {
                editText.requestFocus()
                val imm = appContext.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
            true
        }.onFailure {
            Log.e(TAG, "Không thể hiển thị QuickReplyOverlay", it)
            currentView = null
            currentWindowManager = null
        }.getOrDefault(false)
    }

    private fun sendReply(appContext: Context, session: QuickReplySession, replyText: String) {
        val replyIntent = Intent()
        val bundle = Bundle().apply {
            putCharSequence(session.remoteInput.resultKey, replyText)
        }
        RemoteInput.addResultsToIntent(arrayOf(session.remoteInput), replyIntent, bundle)

        runCatching {
            session.replyAction.actionIntent.send(appContext, 0, replyIntent)
        }.onFailure {
            Log.e(TAG, "Gửi RemoteInput thất bại cho ${session.sourcePkg}", it)
        }

        runCatching {
            val cancelIntent = Intent(appContext, PlaygroundService::class.java).apply {
                action = PlaygroundService.ACTION_CANCEL
                putExtra("id", session.cardId)
                putExtra("oi_scene", session.scene)
            }
            appContext.startService(cancelIntent)
        }.onFailure {
            Log.w(TAG, "Hủy thẻ đảo thất bại cho id=${session.cardId}", it)
        }

        Toast.makeText(appContext, "Đã gửi trả lời", Toast.LENGTH_SHORT).show()
    }

    private fun dismissOnMainThread() {
        val view = currentView
        val wm = currentWindowManager
        currentView = null
        currentWindowManager = null
        if (view != null) {
            if (wm != null) {
                try {
                    wm.removeViewImmediate(view)
                } catch (t: Throwable) {
                    Log.w(TAG, "removeViewImmediate failed", t)
                }
            }
            if (view is ViewGroup) {
                view.removeAllViews()
            }
        }
    }
}
