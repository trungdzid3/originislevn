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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.originisle.android.island.OriginIslandBuilder
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.ui.ActivityScreen
import com.originisle.android.ui.AppsScreen
import com.originisle.android.ui.HomeScreen
import com.originisle.android.ui.IsleColors
import com.originisle.android.ui.ModulesScreen
import com.originisle.android.ui.OnboardingScreen
import com.originisle.android.ui.OriginIsleTheme
import com.originisle.android.ui.PREFS_NAME
import com.originisle.android.ui.SettingsScreen
import com.originisle.android.ui.UpdateChecker
import com.originisle.android.ui.UpdateDialog
import com.originisle.android.ui.samples.IslandSamples
import java.io.File

/**
 * Driver UI: onboard the required permissions, grant access, toggle casting, fire sample cards, and
 * pick which installed apps are allowed on the island.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Triệt tiêu vĩnh viễn bẫy màn hình đen xập nguồn:
        // Tự động xoá file log crash cũ nếu có, luôn khởi tạo initComposeUI bình thường
        runCatching { File(filesDir, "last_crash.txt").delete() }
        runCatching { getSharedPreferences("crash_log", 0).edit().clear().apply() }

        initComposeUI()
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
                "football" -> IslandSamples.footballDemo(this)
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

private enum class Page { HOME, MODULES, SETTINGS, APPS, ACTIVITY }

@Composable
private fun Screen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    remember { OriginIslandBuilder.grantScenes(context) } // whitelist scenes once on entry
    var onboardingDone by remember { mutableStateOf(prefs.getBoolean("onboarding_done", false)) }
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var lastTab by rememberSaveable { mutableStateOf(Page.HOME) }
    var autoUpdateRelease by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }

    // Tự động kiểm tra bản cập nhật khi mở ứng dụng với cơ chế Cooldown (6 giờ)
    LaunchedEffect(Unit) {
        val lastCheck = prefs.getLong("last_update_check_time", 0L)
        val now = System.currentTimeMillis()
        val cooldownMs = 6 * 60 * 60 * 1000L // 6 tiếng
        if (now - lastCheck >= cooldownMs) {
            prefs.edit().putLong("last_update_check_time", now).apply()
            when (val res = UpdateChecker.check(context)) {
                is UpdateChecker.Result.UpdateAvailable -> {
                    autoUpdateRelease = res.release
                }
                else -> {}
            }
        }
    }

    val isSubPage = page == Page.APPS || page == Page.ACTIVITY
    BackHandler(enabled = onboardingDone && page != Page.HOME) {
        page = if (isSubPage) lastTab else Page.HOME
    }

    if (onboardingDone) {
        autoUpdateRelease?.let { release ->
            UpdateDialog(
                release = release,
                onDismiss = { autoUpdateRelease = null },
            )
        }
    }

    Scaffold(
        containerColor = IsleColors.Background,
        bottomBar = {
            if (onboardingDone && !isSubPage) {
                Column {
                    HorizontalDivider(thickness = 0.5.dp, color = IsleColors.BorderHairline)
                    NavigationBar(
                        containerColor = IsleColors.Surface,
                        tonalElevation = 0.dp,
                    ) {
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val tabs = listOf(
                            Triple(Page.HOME, "Trang chủ", NavHomeIcon),
                            Triple(Page.MODULES, "Tính năng", NavModulesIcon),
                            Triple(Page.SETTINGS, "Cài đặt", NavSettingsIcon),
                        )
                        tabs.forEach { (tabPage, label, icon) ->
                            val selected = page == tabPage
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    page = tabPage
                                    lastTab = tabPage
                                },
                                icon = { Icon(icon, contentDescription = label) },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    )
                                },
                                colors = navColors,
                            )
                        }
                    }
                }
            }
        },
    ) { inner ->
        Box(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.fillMaxSize().widthIn(max = 640.dp)) {
                if (!onboardingDone) {
                    OnboardingScreen(context, prefs) { onboardingDone = true }
                    return@Box
                }
                when (page) {
                    Page.HOME -> HomeScreen(
                        context = context,
                        prefs = prefs,
                        onRedoSetup = { onboardingDone = false },
                        onOpenApps = {
                            lastTab = Page.HOME
                            page = Page.APPS
                        },
                        onOpenActivity = {
                            lastTab = Page.HOME
                            page = Page.ACTIVITY
                        },
                    )
                    Page.MODULES -> ModulesScreen(
                        context = context,
                        prefs = prefs,
                    )
                    Page.SETTINGS -> SettingsScreen(
                        context = context,
                        prefs = prefs,
                        onRedoSetup = { onboardingDone = false },
                    )
                    Page.APPS -> AppsScreen(context, prefs, onBack = { page = lastTab })
                    Page.ACTIVITY -> ActivityScreen(context, onBack = { page = lastTab })
                }
            }
        }
    }
}

private val NavHomeIcon: ImageVector by lazy {
    filledNavIcon(
        "nav_home",
        "M10 19v-5h4v5c0 .55.45 1 1 1h3c.55 0 1-.45 1-1v-7h1.7c.46 0 .68-.57.33-.87L12.67 3.6c-.38-.34-.96-.34-1.34 0l-8.36 7.53c-.34.3-.13.87.33.87H5v7c0 .55.45 1 1 1h3c.55 0 1-.45 1-1z",
    )
}

private val NavModulesIcon: ImageVector by lazy {
    filledNavIcon(
        "nav_modules",
        "M4 11h6c.55 0 1-.45 1-1V4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1zm0 10h6c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1zm10 0h6c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1h-6c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1zm0-18v6c0 .55.45 1 1 1h6c.55 0 1-.45 1-1V4c0-.55-.45-1-1-1h-6c-.55 0-1 .45-1 1z",
    )
}

private val NavSettingsIcon: ImageVector by lazy {
    filledNavIcon(
        "nav_settings",
        "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z",
    )
}

private fun filledNavIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(androidx.compose.ui.graphics.Color.Black),
        )
    }.build()
