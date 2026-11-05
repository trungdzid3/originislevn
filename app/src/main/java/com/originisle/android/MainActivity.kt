package com.originisle.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.originisle.android.cards.SportsCard
import com.originisle.android.island.OriginIslandBuilder
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.ui.ActivityScreen
import com.originisle.android.ui.AppsScreen
import com.originisle.android.ui.HomeScreen
import com.originisle.android.ui.IsleColors
import com.originisle.android.ui.OnboardingScreen
import com.originisle.android.ui.OriginIsleTheme
import com.originisle.android.ui.PREFS_NAME
import com.originisle.android.ui.samples.IslandSamples
import java.io.File

/**
 * Driver UI: onboard the required permissions, grant access, toggle casting, fire sample cards, and
 * pick which installed apps are allowed on the island.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiem tra su co khoi dong hoac crash truoc do
        val crashFile = File(filesDir, "last_crash.txt")
        val crashFromSp = getSharedPreferences("crash_log", 0).getString("trace", null)
        val crashTrace = when {
            crashFile.exists() && crashFile.length() > 0 -> runCatching { crashFile.readText() }.getOrNull()
            !crashFromSp.isNullOrBlank() -> crashFromSp
            else -> null
        }

        if (!crashTrace.isNullOrBlank()) {
            showCrashScreen(crashTrace)
        } else {
            initComposeUI()
        }
    }

    private fun showCrashScreen(trace: String) {
        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.parseColor("#0A0D14"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(32), dp(20), dp(32))
        }

        val titleView = TextView(this).apply {
            text = "[CH\u1EA2N \u0110O\u00C1N L\u1ED6I CRASH ORIGIN ISLE]"
            setTextColor(Color.parseColor("#FF5247"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(12))
        }
        container.addView(titleView)

        val descView = TextView(this).apply {
            text = "\u1EE8ng d\u1EE5ng \u0111\u00E3 b\u1EAFt \u0111\u01B0\u1EE3c s\u1EF1 c\u1ED1 nghi\u00EAm tr\u1ECDng tr\u01B0\u1EDBc \u0111\u00F3 ho\u1EB7c trong qu\u00E1 tr\u00ECnh n\u1EA1p giao di\u1EC7n. Chi ti\u1EBFt l\u1ED7i:"
            setTextColor(Color.parseColor("#8E8E93"))
            textSize = 13f
            setPadding(0, 0, 0, dp(16))
        }
        container.addView(descView)

        val copyButton = Button(this).apply {
            text = "SAO CH\u00C9P L\u1ED6I"
            setBackgroundColor(Color.parseColor("#1C1F2E"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                runCatching {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Crash Log", trace))
                    Toast.makeText(this@MainActivity, "\u0110\u00E3 sao ch\u00E9p l\u1ED7i v\u00E0o b\u1ED9 nh\u1EDB t\u1EA1m", Toast.LENGTH_SHORT).show()
                }
            }
        }
        container.addView(copyButton)

        val spacer1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8))
        }
        container.addView(spacer1)

        val continueButton = Button(this).apply {
            text = "XO\u00C1 L\u1ED6I & TI\u1EBEP T\u1EE4C V\u00C0O APP"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                runCatching { File(filesDir, "last_crash.txt").delete() }
                runCatching { getSharedPreferences("crash_log", 0).edit().clear().commit() }
                initComposeUI()
            }
        }
        container.addView(continueButton)

        val spacer2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(16))
        }
        container.addView(spacer2)

        val traceView = TextView(this).apply {
            text = trace
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setBackgroundColor(Color.parseColor("#121620"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setTextIsSelectable(true)
        }
        container.addView(traceView)

        scrollView.addView(container)
        setContentView(scrollView)
    }

    private fun initComposeUI() {
        runCatching {
            when (intent?.getStringExtra("autofire")) {
                "football" -> SportsCard.post(this, "ARS", 1, "MAN", 1, "65'")
            }
            intent?.getIntExtra("autofire_sample", -1)?.takeIf { it >= 0 }?.let {
                IslandSamples.all.getOrNull(it)?.post(this)
            }
        }.onFailure { Log.e("MainActivity", "Loi xu ly intent", it) }

        try {
            setContent {
                OriginIsleTheme {
                    Screen()
                }
            }
        } catch (t: Throwable) {
            Log.e("MainActivity", "Loi khoi tao setContent", t)
            showCrashScreen(Log.getStackTraceString(t))
        }
    }

    /**
     * Heal a killed listener on every foregrounding, not just a cold start — resuming an
     * already-running task would otherwise skip the recovery. Cheap: [forceRebind] no-ops while the
     * listener is connected.
     */
    override fun onResume() {
        super.onResume()
        runCatching {
            if (getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("cast_notifications", true)) {
                PlaygroundService.keepAlive(this)
                NotificationCastListener.forceRebind(this)
            }
        }.onFailure { Log.e("MainActivity", "Loi onResume keepAlive/forceRebind", it) }
    }
}

private enum class Page { HOME, APPS, ACTIVITY }

@Composable
private fun Screen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    remember { OriginIslandBuilder.grantScenes(context) } // whitelist scenes once on entry
    var onboardingDone by remember { mutableStateOf(prefs.getBoolean("onboarding_done", false)) }
    var page by rememberSaveable { mutableStateOf(Page.HOME) }

    // Apps and Activity are pushed on top of Home; system back returns to it.
    BackHandler(enabled = onboardingDone && page != Page.HOME) { page = Page.HOME }

    Scaffold(containerColor = IsleColors.Background) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            if (!onboardingDone) {
                OnboardingScreen(context, prefs) { onboardingDone = true }
                return@Box
            }
            when (page) {
                Page.HOME -> HomeScreen(
                    context, prefs,
                    onRedoSetup = { onboardingDone = false },
                    onOpenApps = { page = Page.APPS },
                    onOpenActivity = { page = Page.ACTIVITY },
                )
                Page.APPS -> AppsScreen(context, prefs, onBack = { page = Page.HOME })
                Page.ACTIVITY -> ActivityScreen(context, onBack = { page = Page.HOME })
            }
        }
    }
}
