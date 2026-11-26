package com.originisle.android.tiktok

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.originisle.android.cards.TikTokStreakCard

/**
 * Activity trong suốt trung gian khi người dùng chạm vào thẻ hoặc nút "Giữ chuỗi" trên Đảo Origin.
 * Sao chép tin nhắn chuỗi vào Clipboard rồi khởi chạy TikTok từ ngữ cảnh Activity hợp lệ
 * (tránh bị Android 14+ BAL chặn khi gọi từ BroadcastReceiver).
 */
class TikTokStreakLaunchActivity : Activity() {

    companion object {
        private const val PREFS_KEY_MESSAGE = "tiktok_streak_custom_message"
        private const val DEFAULT_STREAK_MESSAGE = "🔥"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContext = applicationContext
        val customMsg = getStreakMessage(appContext)

        runCatching {
            val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("tiktok_streak", customMsg))
        }

        Toast.makeText(appContext, "Đã sao chép tin nhắn chuỗi: $customMsg", Toast.LENGTH_SHORT).show()

        val targetPi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(TikTokStreakActionReceiver.EXTRA_TARGET_INTENT, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(TikTokStreakActionReceiver.EXTRA_TARGET_INTENT) as? PendingIntent
        }
        val sourcePkg = intent.getStringExtra(TikTokStreakActionReceiver.EXTRA_SOURCE_PKG)

        var launched = false
        if (targetPi != null) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val opts = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                        )
                        .toBundle()
                    targetPi.send(this, 0, null, null, null, null, opts)
                } else {
                    targetPi.send()
                }
                launched = true
            }
        }

        if (!launched) {
            val candidates = listOfNotNull(
                sourcePkg?.takeIf { it.isNotBlank() },
                "com.ss.android.ugc.trill",
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.aweme",
            ).distinct()

            for (pkg in candidates) {
                val launchIntent = runCatching {
                    packageManager.getLaunchIntentForPackage(pkg)
                }.getOrNull()
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    runCatching { startActivity(launchIntent) }
                    break
                }
            }
        }

        TikTokStreakCard.clear(appContext)
        finish()
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
