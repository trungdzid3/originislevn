package com.originisle.android.service

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Activity cầu nối trong suốt để khởi chạy [QuickReplyOverlay] từ nút bấm trên Đảo.
 * Nếu người dùng tắt tính năng hoặc chưa cấp quyền overlay, tự động fallback về app nguồn.
 */
class QuickReplyActivity : Activity() {

    companion object {
        private const val PREFS = "experimental_prefs"
        private const val KEY_QUICK_REPLY_OVERLAY = "cast_quick_reply_overlay"
        private const val EXTRA_SESSION_ID = "extra_quick_reply_session_id"
        private const val MAX_SESSIONS = 20

        private val sessions = ConcurrentHashMap<String, QuickReplySession>()
        private val sessionOrder = ArrayDeque<String>()

        fun registerSession(session: QuickReplySession): String {
            val sessionId = UUID.randomUUID().toString()
            synchronized(sessionOrder) {
                sessions[sessionId] = session
                sessionOrder.addLast(sessionId)
                while (sessionOrder.size > MAX_SESSIONS) {
                    val oldest = sessionOrder.removeFirst()
                    sessions.remove(oldest)
                }
            }
            return sessionId
        }

        fun createPendingIntent(context: Context, sessionId: String): PendingIntent {
            val intent = Intent(context, QuickReplyActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            return PendingIntent.getActivity(
                context,
                sessionId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)
        val session = sessionId?.let { id ->
            synchronized(sessionOrder) {
                sessionOrder.remove(id)
                sessions.remove(id)
            }
        }

        if (session == null) {
            finish()
            return
        }

        val enabled = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_QUICK_REPLY_OVERLAY, false)

        if (enabled && QuickReplyOverlay.canShowOverlay(this)) {
            QuickReplyOverlay.show(applicationContext, session)
            finish()
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(session.sourcePkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchIntent != null) {
            runCatching { startActivity(launchIntent) }
        }
        finish()
    }
}
