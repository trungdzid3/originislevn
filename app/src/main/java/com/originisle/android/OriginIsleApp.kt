package com.originisle.android

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.originisle.android.island.OriginIslandBuilder
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.sports.SportsFeed
import com.originisle.android.ui.PREFS_NAME
import java.io.File

class OriginIsleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val trace = Log.getStackTraceString(throwable)
            Log.e("OriginIsleApp", "Uncaught exception on thread ${thread.name}: $trace")
            runCatching { File(filesDir, "last_crash.txt").writeText(trace) }
            runCatching { getSharedPreferences("crash_log", 0).edit().putString("trace", trace).commit() }
            // Khong goi killProcess hay System.exit(10) de tranh vo tinh ban chet tien trinh
            defaultHandler?.uncaughtException(thread, throwable)
        }

        runCatching {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val themeMode = prefs.getString("app_theme", "DARK") ?: "DARK"
            val nightMode = when (themeMode) {
                "LIGHT" -> AppCompatDelegate.MODE_NIGHT_NO
                "SYSTEM" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_YES
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }.onFailure { Log.e("OriginIsleApp", "Loi setDefaultNightMode", it) }

        runCatching {
            SportsFeed.init(this)
        }.onFailure { Log.e("OriginIsleApp", "Loi SportsFeed.init", it) }

        runCatching {
            OriginIslandBuilder.grantScenes(this)
        }.onFailure { Log.e("OriginIsleApp", "Loi OriginIslandBuilder.grantScenes", it) }

        // Every process start, not just the UI's: a service restart is the likeliest way back in
        // after the kill that could have left the component half-toggled.
        runCatching {
            NotificationCastListener.ensureEnabled(this)
        }.onFailure { Log.e("OriginIsleApp", "Loi NotificationCastListener.ensureEnabled", it) }
    }
}
