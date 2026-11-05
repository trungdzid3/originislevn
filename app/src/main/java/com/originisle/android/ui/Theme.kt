package com.originisle.android.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.originisle.android.R

/**
 * Bảng màu & Thiết kế theo chuẩn Vivo OriginOS 6 (Atomic Design System).
 * Hỗ trợ chế độ Sáng (LIGHT), Tối (DARK) và Theo hệ thống (SYSTEM).
 */
data class IsleColorPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceHigher: Color,
    val menu: Color,
    val borderHairline: Color,
    val borderActive: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textChip: Color,
    val chevron: Color,
    val accent: Color,
    val glowAccent: Color,
    val accentText: Color,
    val switchOff: Color,
    val divider: Color,
    val danger: Color,
    val good: Color,
    val goodText: Color,
    val goodBg: Color,
    val goodButton: Color,
    val goodSubtle: Color,
    val goodIconBg: Color,
    val bad: Color,
    val badText: Color,
    val badBg: Color,
    val badButton: Color,
    val badSubtle: Color,
    val warn: Color,
    val warnBg: Color,
    val castTagBg: Color,
    val castTagFg: Color,
    val previewBg: Color,
    val previewLabel: Color,
    val tileBlue: Color,
    val tileOrange: Color,
    val tileTeal: Color,
    val tilePurple: Color,
    val tileGreen: Color,
    val tileAmber: Color,
    val tileGray: Color,
)

val DarkPalette = IsleColorPalette(
    isDark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF17181C),
    surfaceHigh = Color(0xFF22242A),
    surfaceHigher = Color(0xFF2C2E35),
    menu = Color(0xFF1C1E24),
    borderHairline = Color(0x1FFFFFFF),
    borderActive = Color(0x3800BFA5),
    text = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFF6B6E7A),
    textChip = Color(0xFFD5D7DF),
    chevron = Color(0xFF60636E),
    accent = Color(0xFF00BFA5),
    glowAccent = Color(0xFF33DFCB),
    accentText = Color(0xFF00D2B8),
    switchOff = Color(0xFF2C2D35),
    divider = Color(0xFF24262E),
    danger = Color(0xFFFF5247),
    good = Color(0xFF00D084),
    goodText = Color(0xFF33DE9E),
    goodBg = Color(0xFF0E2218),
    goodButton = Color(0xFF153324),
    goodSubtle = Color(0xFF8CD8B8),
    goodIconBg = Color(0xFF122C1F),
    bad = Color(0xFFFF5247),
    badText = Color(0xFFFF7A73),
    badBg = Color(0xFF261211),
    badButton = Color(0xFF381A18),
    badSubtle = Color(0xFFD49390),
    warn = Color(0xFFFF9F2E),
    warnBg = Color(0xFF261A0C),
    castTagBg = Color(0xFF1A2645),
    castTagFg = Color(0xFF8EABFF),
    previewBg = Color(0xFF0D0F16),
    previewLabel = Color(0xFF8E95AA),
    tileBlue = Color(0xFF00BFA5),
    tileOrange = Color(0xFF00BFA5),
    tileTeal = Color(0xFF00BFA5),
    tilePurple = Color(0xFF00BFA5),
    tileGreen = Color(0xFF00BFA5),
    tileAmber = Color(0xFF00BFA5),
    tileGray = Color(0xFF454854),
)

val LightPalette = IsleColorPalette(
    isDark = false,
    background = Color(0xFFF2F4F7),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFE8EAEF),
    surfaceHigher = Color(0xFFDFE2E8),
    menu = Color(0xFFFFFFFF),
    borderHairline = Color(0x0F000000),
    borderActive = Color(0x2800BFA5),
    text = Color(0xFF17181D),
    textSecondary = Color(0xFF6C707A),
    textTertiary = Color(0xFF8E929E),
    textChip = Color(0xFF4B4F58),
    chevron = Color(0xFFB0B4BE),
    accent = Color(0xFF00BFA5),
    glowAccent = Color(0xFF00A892),
    accentText = Color(0xFF00897B),
    switchOff = Color(0xFFD1D5DB),
    divider = Color(0xFFE5E7EB),
    danger = Color(0xFFE53935),
    good = Color(0xFF00A86B),
    goodText = Color(0xFF008A56),
    goodBg = Color(0xFFE6F7F0),
    goodButton = Color(0xFFC8EEDD),
    goodSubtle = Color(0xFF008A56),
    goodIconBg = Color(0xFFD4F3E4),
    bad = Color(0xFFE53935),
    badText = Color(0xFFD32F2F),
    badBg = Color(0xFFFFEBEE),
    badButton = Color(0xFFFFCDD2),
    badSubtle = Color(0xFFC62828),
    warn = Color(0xFFF57C00),
    warnBg = Color(0xFFFFF3E0),
    castTagBg = Color(0xFFE8EEFF),
    castTagFg = Color(0xFF2A5BD7),
    previewBg = Color(0xFFE9ECF2),
    previewLabel = Color(0xFF606470),
    tileBlue = Color(0xFF00BFA5),
    tileOrange = Color(0xFF00BFA5),
    tileTeal = Color(0xFF00BFA5),
    tilePurple = Color(0xFF00BFA5),
    tileGreen = Color(0xFF00BFA5),
    tileAmber = Color(0xFF00BFA5),
    tileGray = Color(0xFF8E929E),
)

val LocalIsleColors = staticCompositionLocalOf { DarkPalette }

object IsleColors {
    val current: IsleColorPalette @Composable get() = LocalIsleColors.current
    val isDark: Boolean @Composable get() = LocalIsleColors.current.isDark

    val Background: Color @Composable get() = LocalIsleColors.current.background
    val Surface: Color @Composable get() = LocalIsleColors.current.surface
    val SurfaceHigh: Color @Composable get() = LocalIsleColors.current.surfaceHigh
    val SurfaceHigher: Color @Composable get() = LocalIsleColors.current.surfaceHigher
    val Menu: Color @Composable get() = LocalIsleColors.current.menu

    // Viền hairline siêu mảnh chuẩn OriginOS 6
    val BorderHairline: Color @Composable get() = LocalIsleColors.current.borderHairline
    val BorderActive: Color @Composable get() = LocalIsleColors.current.borderActive

    val Text: Color @Composable get() = LocalIsleColors.current.text
    val TextSecondary: Color @Composable get() = LocalIsleColors.current.textSecondary
    val TextTertiary: Color @Composable get() = LocalIsleColors.current.textTertiary
    val TextChip: Color @Composable get() = LocalIsleColors.current.textChip
    val Chevron: Color @Composable get() = LocalIsleColors.current.chevron

    // Vivo OriginOS 6 Teal / Cyan Mint accent (#00BFA5)
    val Accent: Color @Composable get() = LocalIsleColors.current.accent
    val GlowAccent: Color @Composable get() = LocalIsleColors.current.glowAccent
    val AccentText: Color @Composable get() = LocalIsleColors.current.accentText
    val SwitchOff: Color @Composable get() = LocalIsleColors.current.switchOff
    val Divider: Color @Composable get() = LocalIsleColors.current.divider
    val Danger: Color @Composable get() = LocalIsleColors.current.danger

    val Good: Color @Composable get() = LocalIsleColors.current.good
    val GoodText: Color @Composable get() = LocalIsleColors.current.goodText
    val GoodBg: Color @Composable get() = LocalIsleColors.current.goodBg
    val GoodButton: Color @Composable get() = LocalIsleColors.current.goodButton
    val GoodSubtle: Color @Composable get() = LocalIsleColors.current.goodSubtle
    val GoodIconBg: Color @Composable get() = LocalIsleColors.current.goodIconBg

    val Bad: Color @Composable get() = LocalIsleColors.current.bad
    val BadText: Color @Composable get() = LocalIsleColors.current.badText
    val BadBg: Color @Composable get() = LocalIsleColors.current.badBg
    val BadButton: Color @Composable get() = LocalIsleColors.current.badButton
    val BadSubtle: Color @Composable get() = LocalIsleColors.current.badSubtle

    val Warn: Color @Composable get() = LocalIsleColors.current.warn
    val WarnBg: Color @Composable get() = LocalIsleColors.current.warnBg

    val CastTagBg: Color @Composable get() = LocalIsleColors.current.castTagBg
    val CastTagFg: Color @Composable get() = LocalIsleColors.current.castTagFg
    val PreviewBg: Color @Composable get() = LocalIsleColors.current.previewBg
    val PreviewLabel: Color @Composable get() = LocalIsleColors.current.previewLabel

    // Biểu tượng thẻ chức năng (OriginOS Atomic Tile Colors) - Teal chuẩn
    val TileBlue: Color @Composable get() = LocalIsleColors.current.tileBlue
    val TileOrange: Color @Composable get() = LocalIsleColors.current.tileOrange
    val TileTeal: Color @Composable get() = LocalIsleColors.current.tileTeal
    val TilePurple: Color @Composable get() = LocalIsleColors.current.tilePurple
    val TileGreen: Color @Composable get() = LocalIsleColors.current.tileGreen
    val TileAmber: Color @Composable get() = LocalIsleColors.current.tileAmber
    val TileGray: Color @Composable get() = LocalIsleColors.current.tileGray
}

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val JakartaSans = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

/**
 * Vivo Sans system font family: Automatically loads Vivo's official system font "vivo-sans"
 * on Vivo OriginOS devices, falling back gracefully to Jakarta Sans on other systems.
 */
val VivoSans: FontFamily = runCatching {
    val vivoTypeface = android.graphics.Typeface.create("vivo-sans", android.graphics.Typeface.NORMAL)
    if (vivoTypeface != android.graphics.Typeface.DEFAULT) {
        FontFamily(androidx.compose.ui.text.font.Typeface(vivoTypeface))
    } else {
        JakartaSans
    }
}.getOrElse { JakartaSans }

private fun TextStyle.originFont() = copy(fontFamily = VivoSans)

private val IsleTypography = Typography().run {
    copy(
        displayLarge = displayLarge.originFont(), displayMedium = displayMedium.originFont(),
        displaySmall = displaySmall.originFont(), headlineLarge = headlineLarge.originFont(),
        headlineMedium = headlineMedium.originFont(), headlineSmall = headlineSmall.originFont(),
        titleLarge = titleLarge.originFont(), titleMedium = titleMedium.originFont(),
        titleSmall = titleSmall.originFont(), bodyLarge = bodyLarge.originFont(),
        bodyMedium = bodyMedium.originFont(), bodySmall = bodySmall.originFont(),
        labelLarge = labelLarge.originFont(), labelMedium = labelMedium.originFont(),
        labelSmall = labelSmall.originFont(),
    )
}

@Composable
fun OriginIsleTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var themePref by remember { mutableStateOf(prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM") }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "app_theme") {
                themePref = sp.getString("app_theme", "SYSTEM") ?: "SYSTEM"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val isDark = when (themePref) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val palette = if (isDark) DarkPalette else LightPalette
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            secondary = palette.good,
            onSecondary = Color.White,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceHigh,
            onSurfaceVariant = palette.textTertiary,
            surfaceContainer = palette.menu,
            outline = palette.switchOff,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            secondary = palette.good,
            onSecondary = Color.White,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceHigh,
            onSurfaceVariant = palette.textTertiary,
            surfaceContainer = palette.menu,
            outline = palette.switchOff,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalIsleColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = IsleTypography,
            content = content,
        )
    }
}
