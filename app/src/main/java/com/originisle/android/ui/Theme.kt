package com.originisle.android.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
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
 * Đặc trưng: Nền Obsidian sâu, thẻ Atomic Card bo tròn siêu elip với viền mảnh hairline bán trong suốt.
 */
object IsleColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF17181C)
    val SurfaceHigh = Color(0xFF22242A)
    val SurfaceHigher = Color(0xFF2C2E35)
    val Menu = Color(0xFF1C1E24)

    // Viền hairline siêu mảnh chuẩn OriginOS 6
    val BorderHairline = Color(0x1FFFFFFF)
    val BorderActive = Color(0x382A67FF)

    val Text = Color(0xFFF6F7FA)
    val TextSecondary = Color(0xFF8B8E99)
    val TextTertiary = Color(0xFF6B6E7A)
    val TextChip = Color(0xFFD5D7DF)
    val Chevron = Color(0xFF60636E)

    // Vivo OriginOS 6 Teal / Cyan Mint accent
    val Accent = Color(0xFF00BFA5)
    val GlowAccent = Color(0xFF33DFCB)
    val AccentText = Color(0xFF00D2B8)
    val SwitchOff = Color(0xFF2C2D35)
    val Danger = Color(0xFFFF5247)

    val Good = Color(0xFF00D084)
    val GoodText = Color(0xFF33DE9E)
    val GoodBg = Color(0xFF0E2218)
    val GoodButton = Color(0xFF153324)
    val GoodSubtle = Color(0xFF8CD8B8)
    val GoodIconBg = Color(0xFF122C1F)

    val Bad = Color(0xFFFF5247)
    val BadText = Color(0xFFFF7A73)
    val BadBg = Color(0xFF261211)
    val BadButton = Color(0xFF381A18)
    val BadSubtle = Color(0xFFD49390)

    val Warn = Color(0xFFFF9F2E)
    val WarnBg = Color(0xFF261A0C)

    val CastTagBg = Color(0xFF1A2645)
    val CastTagFg = Color(0xFF8EABFF)
    val PreviewBg = Color(0xFF0D0F16)
    val PreviewLabel = Color(0xFF8E95AA)

    // Biểu tượng thẻ chức năng (OriginOS Atomic Tile Colors)
    val TileBlue = Color(0xFF00BFA5)
    val TileOrange = Color(0xFF00BFA5)
    val TileTeal = Color(0xFF00BFA5)
    val TilePurple = Color(0xFF00BFA5)
    val TileGreen = Color(0xFF00BFA5)
    val TileAmber = Color(0xFF00BFA5)
    val TileGray = Color(0xFF454854)
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

private fun TextStyle.jakarta() = copy(fontFamily = JakartaSans)

private val IsleTypography = Typography().run {
    copy(
        displayLarge = displayLarge.jakarta(), displayMedium = displayMedium.jakarta(),
        displaySmall = displaySmall.jakarta(), headlineLarge = headlineLarge.jakarta(),
        headlineMedium = headlineMedium.jakarta(), headlineSmall = headlineSmall.jakarta(),
        titleLarge = titleLarge.jakarta(), titleMedium = titleMedium.jakarta(),
        titleSmall = titleSmall.jakarta(), bodyLarge = bodyLarge.jakarta(),
        bodyMedium = bodyMedium.jakarta(), bodySmall = bodySmall.jakarta(),
        labelLarge = labelLarge.jakarta(), labelMedium = labelMedium.jakarta(),
        labelSmall = labelSmall.jakarta(),
    )
}

private val OriginIsleColorScheme = darkColorScheme(
    primary = IsleColors.Accent,
    onPrimary = Color.White,
    secondary = IsleColors.Good,
    onSecondary = Color.White,
    background = IsleColors.Background,
    onBackground = IsleColors.Text,
    surface = IsleColors.Surface,
    onSurface = IsleColors.Text,
    surfaceVariant = IsleColors.SurfaceHigh,
    onSurfaceVariant = IsleColors.TextTertiary,
    surfaceContainer = IsleColors.Menu,
    outline = IsleColors.SwitchOff,
)

@Composable
fun OriginIsleTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(colorScheme = OriginIsleColorScheme, typography = IsleTypography, content = content)
}
