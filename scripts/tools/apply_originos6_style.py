# -*- coding: utf-8 -*-
import os

BASE_DIR = r"C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\app\src\main\java\com\originisle\android\ui"

# ==========================================
# 1. Update Theme.kt for OriginOS 6 Design
# ==========================================
theme_path = os.path.join(BASE_DIR, "Theme.kt")
originos6_theme = """package com.originisle.android.ui

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
    val Background = Color(0xFF08090C)
    val Surface = Color(0xFF13141A)
    val SurfaceHigh = Color(0xFF1D1F28)
    val SurfaceHigher = Color(0xFF282B37)
    val Menu = Color(0xFF1C1E26)

    // Viền hairline siêu mảnh chuẩn OriginOS 6
    val BorderHairline = Color(0x18FFFFFF)
    val BorderActive = Color(0x382A67FF)

    val Text = Color(0xFFF6F7FA)
    val TextSecondary = Color(0xFF8B8E99)
    val TextTertiary = Color(0xFF6B6E7A)
    val TextChip = Color(0xFFD5D7DF)
    val Chevron = Color(0xFF60636E)

    // Vivo Origin Electric Blue & Accent Colors
    val Accent = Color(0xFF2B66FF)
    val AccentText = Color(0xFF80A6FF)
    val SwitchOff = Color(0xFF2A2D3A)
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
    val TileBlue = Color(0xFF2B66FF)
    val TileOrange = Color(0xFFFF6433)
    val TileTeal = Color(0xFF00BCC4)
    val TilePurple = Color(0xFF784EF8)
    val TileGreen = Color(0xFF00BA74)
    val TileAmber = Color(0xFFFFAE19)
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
"""

with open(theme_path, "w", encoding="utf-8") as f:
    f.write(originos6_theme)
print("1. Updated Theme.kt with OriginOS 6 colors")

# ==========================================
# 2. Update Components.kt for OriginOS 6 Atomic Card design
# ==========================================
comp_path = os.path.join(BASE_DIR, "Components.kt")
with open(comp_path, "r", encoding="utf-8") as f:
    comp_text = f.read()

# Update GroupRadius to 28.dp (OriginOS Squircle)
comp_text = comp_text.replace("private val GroupRadius = 24.dp", "private val GroupRadius = 28.dp")

# Add border to GroupCard
old_group_card = """@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(GroupRadius)).background(IsleColors.Surface),
        content = content,
    )
}"""

new_group_card = """@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GroupRadius))
            .background(IsleColors.Surface)
            .border(
                width = 0.8.dp,
                color = IsleColors.BorderHairline,
                shape = RoundedCornerShape(GroupRadius),
            ),
        content = content,
    )
}"""

comp_text = comp_text.replace(old_group_card, new_group_card)
if "import androidx.compose.foundation.border" not in comp_text:
    comp_text = comp_text.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\\nimport androidx.compose.foundation.border")


# Update SectionLabel to OriginOS 6 Atomic Subhead
old_section_label = """@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(horizontal = 16.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = IsleColors.TextSecondary,
    )
}"""

new_section_label = """@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(horizontal = 18.dp, vertical = 2.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        color = IsleColors.TextTertiary,
    )
}"""

comp_text = comp_text.replace(old_section_label, new_section_label)

# Update SegmentedControl to OriginOS Atomic Tab
old_seg_control = """@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    track: Color = IsleColors.SurfaceHigh,
    height: Dp = 44.dp,
) {"""

new_seg_control = """@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    track: Color = IsleColors.SurfaceHigh,
    height: Dp = 46.dp,
) {"""

comp_text = comp_text.replace(old_seg_control, new_seg_control)

with open(comp_path, "w", encoding="utf-8") as f:
    f.write(comp_text)
print("2. Updated Components.kt for OriginOS 6")

# ==========================================
# 3. Update HomeScreen.kt styling details
# ==========================================
home_path = os.path.join(BASE_DIR, "HomeScreen.kt")
with open(home_path, "r", encoding="utf-8") as f:
    home_text = f.read()

# Replace StatusPill with OriginOS 6 Capsule Pill style
old_status_pill = """@Composable
private fun StatusPill(connected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (connected) IsleColors.GoodBg else IsleColors.BadBg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) IsleColors.Good else IsleColors.Bad))
        Text(
            if (connected) "Đang hoạt động" else "Chưa kết nối",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (connected) IsleColors.GoodText else IsleColors.BadText,
        )
    }
}"""

new_status_pill = """@Composable
private fun StatusPill(connected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (connected) IsleColors.GoodBg else IsleColors.BadBg)
            .border(
                width = 0.8.dp,
                color = if (connected) IsleColors.Good.copy(alpha = 0.3f) else IsleColors.Bad.copy(alpha = 0.3f),
                shape = RoundedCornerShape(17.dp),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) IsleColors.Good else IsleColors.Bad))
        Text(
            if (connected) "Đảo OriginOS: Đang hoạt động" else "Đảo OriginOS: Chưa kết nối",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (connected) IsleColors.GoodText else IsleColors.BadText,
        )
    }
}"""

home_text = home_text.replace(old_status_pill, new_status_pill)
if "import androidx.compose.foundation.border" not in home_text:
    home_text = home_text.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\\nimport androidx.compose.foundation.border")

# Update SampleTester preview box to look like OriginOS 6 Island hole punch
old_sample_box = """.background(IsleColors.PreviewBg)
                .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),"""

new_sample_box = """.background(IsleColors.PreviewBg)
                .border(
                    width = 0.8.dp,
                    color = IsleColors.BorderHairline,
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),"""

home_text = home_text.replace(old_sample_box, new_sample_box)

with open(home_path, "w", encoding="utf-8") as f:
    f.write(home_text)
print("3. Updated HomeScreen.kt with OriginOS 6 styling")

