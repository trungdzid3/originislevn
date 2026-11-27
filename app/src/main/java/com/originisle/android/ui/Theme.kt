package com.originisle.android.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val accentSoft: Color = Color(0x1A0F77FF),
    val link: Color = Color(0xFF456FFF),
    val textSub: Color = Color(0x78000000),
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
    val edgeLightTop: Color,
    val edgeLightBottom: Color,
    val floatingGlassBg: Color,
    val ambientGlow: Color,
)

val LocalIsleColors = staticCompositionLocalOf { FallbackDarkColorScheme.toIsleColorPalette(true) }

object IsleColors {
    val current: IsleColorPalette @Composable get() = LocalIsleColors.current
    val isDark: Boolean @Composable get() = LocalIsleColors.current.isDark

    val Background: Color @Composable get() = LocalIsleColors.current.background
    val Surface: Color @Composable get() = LocalIsleColors.current.surface
    val SurfaceHigh: Color @Composable get() = LocalIsleColors.current.surfaceHigh
    val SurfaceHigher: Color @Composable get() = LocalIsleColors.current.surfaceHigher
    val Menu: Color @Composable get() = LocalIsleColors.current.menu

    // Viền hairline siêu mảnh chuẩn OriginOS 6 (0.8dp)
    val BorderHairline: Color @Composable get() = LocalIsleColors.current.borderHairline
    val BorderActive: Color @Composable get() = LocalIsleColors.current.borderActive

    val Text: Color @Composable get() = LocalIsleColors.current.text
    val TextSecondary: Color @Composable get() = LocalIsleColors.current.textSecondary
    val TextSub: Color @Composable get() = LocalIsleColors.current.textSub
    val TextTertiary: Color @Composable get() = LocalIsleColors.current.textTertiary
    val TextChip: Color @Composable get() = LocalIsleColors.current.textChip
    val Chevron: Color @Composable get() = LocalIsleColors.current.chevron

    // OriginUI ROM 16.0 Accent (#0F77FF mặc định)
    val Accent: Color @Composable get() = LocalIsleColors.current.accent
    val GlowAccent: Color @Composable get() = LocalIsleColors.current.glowAccent
    val AccentSoft: Color @Composable get() = LocalIsleColors.current.accentSoft
    val AccentText: Color @Composable get() = LocalIsleColors.current.accentText
    val Link: Color @Composable get() = LocalIsleColors.current.link
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

    // OriginOS 6-7 Space System 2.0 & Liquid Glass tokens
    val EdgeLightTop: Color @Composable get() = LocalIsleColors.current.edgeLightTop
    val EdgeLightBottom: Color @Composable get() = LocalIsleColors.current.edgeLightBottom
    val FloatingGlassBg: Color @Composable get() = LocalIsleColors.current.floatingGlassBg
    val AmbientGlow: Color @Composable get() = LocalIsleColors.current.ambientGlow
}

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val JakartaSans: FontFamily = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

/**
 * Vivo Sans system font family: Tự động sử dụng font hệ thống Vivo ("vivo-sans" / "sans-serif-medium")
 * trên các thiết bị Vivo/iQOO OriginOS, đồng thời tự điều chỉnh tương thích với FontFamily.SansSerif
 * nhằm đảm bảo tôn trọng 100% tỷ lệ co giãn phông chữ (fontScale), kích thước hiển thị và các biến thể độ đậm.
 */
val VivoSans: FontFamily = runCatching {
    val vivoTypeface = android.graphics.Typeface.create("vivo-sans", android.graphics.Typeface.NORMAL)
    if (vivoTypeface != android.graphics.Typeface.DEFAULT) {
        FontFamily(androidx.compose.ui.text.font.Typeface(vivoTypeface))
    } else {
        FontFamily.SansSerif
    }
}.getOrElse { FontFamily.SansSerif }

private val IsleTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = VivoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
    ),
)

enum class AppThemeColor(val key: String, val label: String, val swatch: Color) {
    DYNAMIC("DYNAMIC", "Tự động", Color(0xFF0F77FF)),
    BLUE("BLUE", "Xanh dương", Color(0xFF0F77FF)),
    TEAL("TEAL", "Xanh ngọc", Color(0xFF00897B)),
    VIOLET("VIOLET", "Tím", Color(0xFF7C4DFF)),
    ROSE("ROSE", "Hồng", Color(0xFFD84374)),
    AMBER("AMBER", "Hổ phách", Color(0xFFD97706));

    companion object {
        fun fromKey(key: String?): AppThemeColor {
            return entries.firstOrNull { it.key == key } ?: DYNAMIC
        }
    }
}

private val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFF2E8BFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0x260F77FF),
    onPrimaryContainer = Color(0xFF7BB8FF),
    secondary = Color(0xFFC1C6DD),
    onSecondary = Color(0xFF2B3042),
    secondaryContainer = Color(0xFF1E222B),
    onSecondaryContainer = Color(0xFFF2F4F8),
    tertiary = Color(0xFFE1BBE0),
    onTertiary = Color(0xFF422744),
    tertiaryContainer = Color(0xFF5A3D5C),
    onTertiaryContainer = Color(0xFFFED7FC),
    background = Color(0xFF0D0F14),
    onBackground = Color(0xFFF2F4F8),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFF2F4F8),
    surfaceVariant = Color(0xFF1E222B),
    onSurfaceVariant = Color(0xB3F2F4F8),
    surfaceContainerLowest = Color(0xFF090B10),
    surfaceContainerLow = Color(0xFF13161D),
    surfaceContainer = Color(0xFF171A21),
    surfaceContainerHigh = Color(0xFF1E222B),
    surfaceContainerHighest = Color(0xFF262B36),
    error = Color(0xFFFF5252),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF471619),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0x4DFFFFFF),
    outlineVariant = Color(0x1FFFFFFF),
)

private val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF0F77FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0x1A0F77FF),
    onPrimaryContainer = Color(0xFF0F77FF),
    secondary = Color(0xFF585E72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8EEF8),
    onSecondaryContainer = Color(0xE6000000),
    tertiary = Color(0xFF456FFF),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0x1A456FFF),
    onTertiaryContainer = Color(0xFF456FFF),
    background = Color(0xFFF2F2F5),
    onBackground = Color(0xE6000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xE6000000),
    surfaceVariant = Color(0xFFF7F8FA),
    onSurfaceVariant = Color(0xCC000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainer = Color(0xFFF2F2F5),
    surfaceContainerHigh = Color(0xFFEBEDF2),
    surfaceContainerHighest = Color(0xFFE3E5EB),
    error = Color(0xFFFF3636),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0x1AFF3636),
    onErrorContainer = Color(0xFFFF3636),
    outline = Color(0x33000000),
    outlineVariant = Color(0x14000000),
)

private val TealDarkColorScheme = darkColorScheme(
    primary = Color(0xFF5CE0CD),
    onPrimary = Color(0xFF003831),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFF7CF8E4),
    secondary = Color(0xFFB1CCC7),
    onSecondary = Color(0xFF1C3531),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCDE8E3),
    tertiary = Color(0xFFADC9E5),
    onTertiary = Color(0xFF143349),
    tertiaryContainer = Color(0xFF2D4961),
    onTertiaryContainer = Color(0xFFCBE6FF),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDDE4E1),
    surface = Color(0xFF0E1513),
    onSurface = Color(0xFFDDE4E1),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    surfaceContainerLowest = Color(0xFF090F0E),
    surfaceContainerLow = Color(0xFF161D1B),
    surfaceContainer = Color(0xFF1A211F),
    surfaceContainerHigh = Color(0xFF252B29),
    surfaceContainerHighest = Color(0xFF303634),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4947),
)

private val TealLightColorScheme = lightColorScheme(
    primary = Color(0xFF006B5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7CF8E4),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4A635E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8E3),
    onSecondaryContainer = Color(0xFF06201C),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCBE6FF),
    onTertiaryContainer = Color(0xFF001E30),
    background = Color(0xFFF5FBF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF5FBF8),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F2),
    surfaceContainer = Color(0xFFE9EFEC),
    surfaceContainerHigh = Color(0xFFE3EAE7),
    surfaceContainerHighest = Color(0xFFDDE4E1),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),
)

private val VioletDarkColorScheme = darkColorScheme(
    primary = Color(0xFFCDBDFF),
    onPrimary = Color(0xFF350097),
    primaryContainer = Color(0xFF4E19D1),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Color(0xFFCBC3DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF494458),
    onSecondaryContainer = Color(0xFFE7DFF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF14121A),
    onBackground = Color(0xFFE6E0ED),
    surface = Color(0xFF14121A),
    onSurface = Color(0xFFE6E0ED),
    surfaceVariant = Color(0xFF484550),
    onSurfaceVariant = Color(0xFFC9C4D2),
    surfaceContainerLowest = Color(0xFF0F0D14),
    surfaceContainerLow = Color(0xFF1C1A22),
    surfaceContainer = Color(0xFF201E27),
    surfaceContainerHigh = Color(0xFF2B2931),
    surfaceContainerHighest = Color(0xFF36333C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F9B),
    outlineVariant = Color(0xFF484550),
)

private val VioletLightColorScheme = lightColorScheme(
    primary = Color(0xFF6538E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8DDFF),
    onPrimaryContainer = Color(0xFF20005E),
    secondary = Color(0xFF615B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7DFF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFDF7FF),
    onBackground = Color(0xFF1C1A22),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1C1A22),
    surfaceVariant = Color(0xFFE6E0EC),
    onSurfaceVariant = Color(0xFF484550),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F1FA),
    surfaceContainer = Color(0xFFF1EBF5),
    surfaceContainerHigh = Color(0xFFEBE6EF),
    surfaceContainerHighest = Color(0xFFE6E0ED),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF797581),
    outlineVariant = Color(0xFFC9C4D2),
)

private val RoseDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB1C6),
    onPrimary = Color(0xFF65002D),
    primaryContainer = Color(0xFF8F0B43),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE2BDC6),
    onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5A3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFEFBC94),
    onTertiary = Color(0xFF48290C),
    tertiaryContainer = Color(0xFF623F20),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF1A1114),
    onBackground = Color(0xFFF0DEE2),
    surface = Color(0xFF1A1114),
    onSurface = Color(0xFFF0DEE2),
    surfaceVariant = Color(0xFF514347),
    onSurfaceVariant = Color(0xFFD5C2C6),
    surfaceContainerLowest = Color(0xFF140C0F),
    surfaceContainerLow = Color(0xFF22191C),
    surfaceContainer = Color(0xFF271D20),
    surfaceContainerHigh = Color(0xFF32272A),
    surfaceContainerHighest = Color(0xFF3D3235),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF9E8C90),
    outlineVariant = Color(0xFF514347),
)

private val RoseLightColorScheme = lightColorScheme(
    primary = Color(0xFFBA1A58),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E001A),
    secondary = Color(0xFF74565F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Color(0xFF7C5635),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF22191C),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF22191C),
    surfaceVariant = Color(0xFFF2DDE2),
    onSurfaceVariant = Color(0xFF514347),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0F3),
    surfaceContainer = Color(0xFFFAEAEE),
    surfaceContainerHigh = Color(0xFFF4E4E8),
    surfaceContainerHighest = Color(0xFFF0DEE2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF837377),
    outlineVariant = Color(0xFFD5C2C6),
)

private val AmberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB95C),
    onPrimary = Color(0xFF472A00),
    primaryContainer = Color(0xFF663E00),
    onPrimaryContainer = Color(0xFFFFDDB5),
    secondary = Color(0xFFDEC2A2),
    onSecondary = Color(0xFF3E2D17),
    secondaryContainer = Color(0xFF56432B),
    onSecondaryContainer = Color(0xFFFCDDBD),
    tertiary = Color(0xFFB9CE9F),
    onTertiary = Color(0xFF253514),
    tertiaryContainer = Color(0xFF3B4C28),
    onTertiaryContainer = Color(0xFFD5EAB9),
    background = Color(0xFF18120B),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF18120B),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    surfaceContainerLowest = Color(0xFF120D07),
    surfaceContainerLow = Color(0xFF201A13),
    surfaceContainer = Color(0xFF251E17),
    surfaceContainerHigh = Color(0xFF302921),
    surfaceContainerHighest = Color(0xFF3B332B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF9B8F80),
    outlineVariant = Color(0xFF4F4539),
)

private val AmberLightColorScheme = lightColorScheme(
    primary = Color(0xFFB56200),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB5),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF705B41),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFCDDBD),
    onSecondaryContainer = Color(0xFF271905),
    tertiary = Color(0xFF52643E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD5EAB9),
    onTertiaryContainer = Color(0xFF111F03),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF201A13),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF201A13),
    surfaceVariant = Color(0xFFEFE0CF),
    onSurfaceVariant = Color(0xFF4F4539),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1E5),
    surfaceContainer = Color(0xFFF9EBDD),
    surfaceContainerHigh = Color(0xFFF3E5D8),
    surfaceContainerHighest = Color(0xFFEDE0D4),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF817567),
    outlineVariant = Color(0xFFD3C4B4),
)

private val FallbackDarkColorScheme = BlueDarkColorScheme
private val FallbackLightColorScheme = BlueLightColorScheme

fun resolveAppIsDarkTheme(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return when (prefs.getString(PREF_APP_THEME, "SYSTEM")) {
        "LIGHT" -> false
        "DARK" -> true
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }
}

fun resolveMaterialColorScheme(context: Context, isDark: Boolean): ColorScheme {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val themeColor = AppThemeColor.fromKey(prefs.getString(PREF_APP_THEME_COLOR, AppThemeColor.DYNAMIC.key))
    return when (themeColor) {
        AppThemeColor.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) FallbackDarkColorScheme else FallbackLightColorScheme
            }
        }
        AppThemeColor.BLUE -> if (isDark) BlueDarkColorScheme else BlueLightColorScheme
        AppThemeColor.TEAL -> if (isDark) TealDarkColorScheme else TealLightColorScheme
        AppThemeColor.VIOLET -> if (isDark) VioletDarkColorScheme else VioletLightColorScheme
        AppThemeColor.ROSE -> if (isDark) RoseDarkColorScheme else RoseLightColorScheme
        AppThemeColor.AMBER -> if (isDark) AmberDarkColorScheme else AmberLightColorScheme
    }
}

private fun ColorScheme.toIsleColorPalette(isDark: Boolean): IsleColorPalette = IsleColorPalette(
    isDark = isDark,
    background = if (isDark) Color(0xFF0D0F14) else Color(0xFFF2F2F5),
    surface = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF),
    surfaceHigh = if (isDark) Color(0xFF1E222B) else Color(0xFFF7F8FA),
    surfaceHigher = if (isDark) Color(0x14FFFFFF) else Color(0x0D000000),
    menu = if (isDark) Color(0xFF1E222B) else Color(0xFFFFFFFF),
    borderHairline = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000),
    borderActive = primary.copy(alpha = 0.35f),
    text = if (isDark) Color(0xFFF2F4F8) else Color(0xE6000000),
    textSecondary = if (isDark) Color(0xB3F2F4F8) else Color(0xCC000000),
    textTertiary = if (isDark) Color(0x73F2F4F8) else Color(0x59000000),
    textChip = if (isDark) Color(0xFFF2F4F8) else Color(0xE6000000),
    chevron = if (isDark) Color(0x73F2F4F8) else Color(0x59000000),
    accent = primary,
    glowAccent = primary.copy(alpha = if (isDark) 0.15f else 0.10f),
    accentText = primary,
    accentSoft = primary.copy(alpha = if (isDark) 0.15f else 0.10f),
    link = if (isDark) Color(0xFF6EA3FF) else Color(0xFF456FFF),
    textSub = if (isDark) Color(0x99F2F4F8) else Color(0x78000000),
    switchOff = if (isDark) Color(0x75464646) else Color(0x59CCCCCC),
    divider = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000),
    danger = if (isDark) error else Color(0xFFFF3636),
    good = if (isDark) Color(0xFF2AD17E) else Color(0xFF23B26D),
    goodText = if (isDark) Color(0xFF3CE08F) else Color(0xFF23B26D),
    goodBg = if (isDark) Color(0xFF0F261A) else Color(0x1A23B26D),
    goodButton = if (isDark) Color(0xFF153624) else Color(0x2923B26D),
    goodSubtle = if (isDark) Color(0xFF8CD8B8) else Color(0xFF23B26D),
    goodIconBg = if (isDark) Color(0xFF122E1F) else Color(0x1F23B26D),
    bad = if (isDark) error else Color(0xFFFF3636),
    badText = if (isDark) onErrorContainer else Color(0xFFFF3636),
    badBg = if (isDark) errorContainer else Color(0x14FF3636),
    badButton = if (isDark) error.copy(alpha = 0.22f) else Color(0x1AFF3636),
    badSubtle = if (isDark) onErrorContainer.copy(alpha = 0.85f) else Color(0xCCFF3636),
    warn = Color(0xFFFF9500),
    warnBg = if (isDark) Color(0xFF261A0C) else Color(0x14FF9500),
    castTagBg = primary.copy(alpha = if (isDark) 0.18f else 0.10f),
    castTagFg = primary,
    previewBg = if (isDark) Color(0xFF1E222B) else Color(0xFFF7F8FA),
    previewLabel = if (isDark) Color(0xB3F2F4F8) else Color(0x78000000),
    tileBlue = primary,
    tileOrange = tertiary,
    tileTeal = primary,
    tilePurple = secondary,
    tileGreen = if (isDark) Color(0xFF2AD17E) else Color(0xFF23B26D),
    tileAmber = Color(0xFFFF9500),
    tileGray = if (isDark) Color(0xB3F2F4F8) else Color(0x78000000),
    edgeLightTop = if (isDark) Color(0x29FFFFFF) else Color(0x14000000),
    edgeLightBottom = if (isDark) Color(0x0AFFFFFF) else Color(0x0A000000),
    floatingGlassBg = if (isDark) Color(0xEB141822) else Color(0xF2FFFFFF),
    ambientGlow = if (isDark) primary.copy(alpha = 0.14f) else primary.copy(alpha = 0.08f),
)

val IsleShapes = Shapes(
    extraSmall = G2RoundedCornerShape(8.dp),
    small = G2RoundedCornerShape(12.dp),
    medium = G2RoundedCornerShape(18.dp),
    large = G2RoundedCornerShape(24.dp),
    extraLarge = G2RoundedCornerShape(30.dp),
)

@Composable
fun OriginIsleTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var themePref by remember { mutableStateOf(prefs.getString(PREF_APP_THEME, "SYSTEM") ?: "SYSTEM") }
    var themeColorPref by remember {
        mutableStateOf(prefs.getString(PREF_APP_THEME_COLOR, AppThemeColor.DYNAMIC.key) ?: AppThemeColor.DYNAMIC.key)
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            when (key) {
                PREF_APP_THEME -> themePref = sp.getString(PREF_APP_THEME, "SYSTEM") ?: "SYSTEM"
                PREF_APP_THEME_COLOR -> {
                    themeColorPref = sp.getString(PREF_APP_THEME_COLOR, AppThemeColor.DYNAMIC.key)
                        ?: AppThemeColor.DYNAMIC.key
                }
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

    val colorScheme = remember(context, isDark, themeColorPref) {
        resolveMaterialColorScheme(context, isDark)
    }
    val palette = colorScheme.toIsleColorPalette(isDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalIsleColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = IsleShapes,
            typography = IsleTypography,
            content = content,
        )
    }
}
