package com.originisle.android.tiktok

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.originisle.android.cards.TikTokStreakCard

/**
 * Receiver xử lý sự kiện bấm nút trên Thẻ Đảo Động Chuỗi TikTok.
 * Tự động sao chép biểu tượng/tin nhắn chuỗi vào bộ nhớ tạm và mở thẳng cuộc trò chuyện TikTok.
 */
class TikTokStreakActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SEND_TIKTOK_STREAK = "com.originisle.android.ACTION_SEND_TIKTOK_STREAK"
        const val EXTRA_TARGET_INTENT = "target_intent"
        const val EXTRA_SOURCE_PKG = "source_pkg"
        private const val PREFS_KEY_MESSAGE = "tiktok_streak_custom_message"
        private const val DEFAULT_STREAK_MESSAGE = "🔥"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != ACTION_SEND_TIKTOK_STREAK) {
            return
        }

        val appContext = context.applicationContext

        // 1. Lấy tin nhắn chuỗi tùy chỉnh từ SharedPreferences (mặc định: 🔥)
        val customMsg = getStreakMessage(appContext)

        // 2. Sao chép vào Clipboard hệ thống
        runCatching {
            val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("tiktok_streak", customMsg))
        }

        // 3. Hiển thị Toast thông báo nhanh
        Toast.makeText(appContext, "Đã sao chép tin nhắn chuỗi: $customMsg", Toast.LENGTH_SHORT).show()

        // 4. Mở trực tiếp màn hình chat TikTok tương ứng
        val targetPi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_TARGET_INTENT, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_TARGET_INTENT) as? PendingIntent
        }
        val sourcePkg = intent.getStringExtra(EXTRA_SOURCE_PKG) ?: "com.ss.android.ugc.trill"

        var launched = false
        if (targetPi != null) {
            runCatching {
                targetPi.send()
                launched = true
            }
        }

        if (!launched) {
            runCatching {
                val launchIntent = appContext.packageManager.getLaunchIntentForPackage(sourcePkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    appContext.startActivity(launchIntent)
                }
            }
        }

        // 5. Đóng thẻ Đảo Động chuỗi
        TikTokStreakCard.clear(appContext)
    }

    private fun getStreakMessage(context: Context): String {
        val expPrefs = context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)
        val expMsg = expPrefs.getString(PREFS_KEY_MESSAGE, null)?.trim()
        if (!expMsg.isNullOrEmpty()) {
            return expMsg
        }

        val streakPrefs = context.getSharedPreferences("tiktok_streak_prefs", Context.MODE_PRIVATE)
        val streakMsg = streakPrefs.getString(PREFS_KEY_MESSAGE, null)?.trim()
        if (!streakMsg.isNullOrEmpty()) {
            return streakMsg
        }

        return DEFAULT_STREAK_MESSAGE
    }
}
