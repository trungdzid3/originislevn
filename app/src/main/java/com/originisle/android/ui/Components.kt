package com.originisle.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared building blocks for OriginOS 6 UI: grouped squircle cards, settings rows, pills, switches. */

/** Page padding shared by every screen. */
val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp)

private val GroupRadius = 24.dp

/** Back button + large title + an optional trailing action, for the Apps and Activity screens. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircleIconButton(IsleIcons.Back, "Back", onBack)
        Text(
            title,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = IsleColors.Text,
        )
        trailing()
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    background: Color = IsleColors.Surface,
    tint: Color = IsleColors.Text,
    iconSize: Dp = 20.dp,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        contentColor = tint,
        modifier = Modifier.size(48.dp).semantics { role = Role.Button },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, Modifier.size(iconSize), tint = tint)
        }
    }
}

/** A full-width (or weighted) rounded button with an optional leading icon. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSize: Dp = 18.dp,
    container: Color = IsleColors.Surface,
    content: Color = IsleColors.Text,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(height / 2),
        color = container,
        contentColor = content,
        modifier = modifier.heightIn(min = height).alpha(if (enabled) 1f else 0.4f).semantics { role = Role.Button },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(iconSize), tint = content)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = content)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = IsleColors.TextSecondary,
    )
}

/** Squircle card with 24dp rounded corners holding a group of settings rows. */
@Composable
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
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
        content = content,
    )
}

/**
 * The shape of row [index] of [count] in a group drawn one row per lazy item: only the outer corners
 * are rounded, so consecutive rows read as one card.
 */
fun groupItemShape(index: Int, count: Int): Shape {
    val top = if (index == 0) GroupRadius else 0.dp
    val bottom = if (index == count - 1) GroupRadius else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** Hairline between rows in OriginOS 6, inset to line up with row titles. */
@Composable
fun GroupDivider(inset: Dp = 56.dp) {
    Box(Modifier.padding(start = inset).fillMaxWidth().height(0.8.dp).background(IsleColors.Divider))
}

/** OriginOS 6 vector icon without heavy background frames. */
@Composable
fun IconTile(icon: ImageVector, color: Color = IsleColors.Accent, size: Dp = 26.dp) {
    Box(
        Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = color)
    }
}

/** Icon + title (+ subtitle) + trailing content. Clickable when [onClick] is set. */
@Composable
fun SettingRow(
    icon: ImageVector,
    tileColor: Color = IsleColors.Accent,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 58.dp)
            .padding(start = 16.dp, end = 14.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon, color = tileColor)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IsleColors.Text)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = IsleColors.TextSecondary, lineHeight = 18.sp)
            }
        }
        trailing()
    }
}

/** A [SettingRow] with a switch; the whole row toggles. */
@Composable
fun ToggleRow(
    icon: ImageVector,
    tileColor: Color = IsleColors.Accent,
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    onChange: (Boolean) -> Unit,
) {
    SettingRow(
        icon, tileColor, title,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        subtitle = subtitle,
    ) {
        IsleSwitch(checked, onCheckedChange = null, modifier = Modifier.padding(end = 4.dp))
    }
}

/** A row that opens another screen: tile, title, optional value, chevron. */
@Composable
fun NavRow(
    icon: ImageVector,
    tileColor: Color = IsleColors.Accent,
    title: String,
    value: String? = null,
    onClick: () -> Unit,
) {
    SettingRow(icon, tileColor, title, onClick = onClick) {
        if (value != null) Text(value, fontSize = 14.sp, color = IsleColors.TextSecondary)
        Icon(IsleIcons.ChevronRight, null, Modifier.padding(end = 6.dp).size(18.dp), tint = IsleColors.Chevron)
    }
}

/**
 * Khung cài đặt lồng nhau chuẩn Vivo OriginOS 6:
 * Khi tùy chọn cha được bật, các tùy chọn con mở ra mượt mà qua AnimatedVisibility,
 * nằm gọn trong card con (SurfaceHigh, bo góc 16.dp, chỉ dẫn tinh tế bên trái),
 * không gây xô lệch hay đứt gãy bố cục ngoài.
 */
@Composable
fun NestedSettingContainer(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top,
        ) + fadeIn(
            animationSpec = tween(durationMillis = 180),
        ),
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top,
        ) + fadeOut(
            animationSpec = tween(durationMillis = 140),
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 2.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(IsleColors.SurfaceHigh)
                .border(
                    width = 0.8.dp,
                    color = IsleColors.BorderHairline,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Chỉ dẫn dọc mảnh tinh tế màu Accent chuẩn OriginOS 6
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(IsleColors.Accent.copy(alpha = 0.6f)),
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content,
                )
            }
        }
    }
}

/**
 * OriginOS 6/7 Custom Switch (100% pixel-matched to Vivo OriginOS 6/7 physical switch):
 * - Track: soft capsule pill (48.dp width x 24.dp height, RoundedCornerShape(12.dp)).
 *   ON: Soft Pastel Cyan / Mint (#A0D0CF).
 *   OFF: Light gray (#E2E6E8 in Light Mode / #2C2F36 in Dark Mode).
 * - Thumb: circular ring (32.dp diameter), overhangs track equally top & bottom by 4.dp.
 *   Inner disk: Pure White (#FFFFFF).
 *   Outer border ring: 4.8.dp thick.
 *   ON: Deep rich Teal (#006D77).
 *   OFF: Slate gray (#8A9096 in Light Mode / #71767C in Dark Mode).
 * Đảm bảo kích thước chạm tối thiểu đạt chuẩn tiếp cận 48.dp.
 */
@Composable
fun IsleSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val trackWidth = 48.dp
    val trackHeight = 24.dp
    val thumbSize = 32.dp
    val isDark = IsleColors.isDark

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "thumbOffset",
    )
    val trackColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) Color(0xFFA0D0CF) else if (isDark) Color(0xFF2C2F36) else Color(0xFFE2E6E8),
        animationSpec = tween(durationMillis = 180),
        label = "trackColor",
    )
    val ringColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) Color(0xFF006D77) else if (isDark) Color(0xFF71767C) else Color(0xFF8A9096),
        animationSpec = tween(durationMillis = 180),
        label = "ringColor",
    )

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Switch,
                        onClick = { onCheckedChange(!checked) },
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(thumbSize),
            contentAlignment = Alignment.CenterStart,
        ) {
            // Track: soft rounded capsule centered horizontally and vertically
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(trackColor)
            )
            // Thumb: pure white center with thick outer ring border that overhangs track
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 4.8.dp,
                        color = ringColor,
                        shape = CircleShape,
                    )
            )
        }
    }
}

/** Segmented control styled for OriginOS 6. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    track: Color = IsleColors.SurfaceHigh,
    height: Dp = 46.dp,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(track)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) IsleColors.SurfaceHigher else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) IsleColors.Text else IsleColors.TextSecondary,
                )
            }
        }
    }
}

/** A small rounded choice button (delay picker, sample picker), min 48.dp touch size. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedContainer: Color = IsleColors.Accent,
    selectedContent: Color = Color.White,
    shape: Shape = RoundedCornerShape(12.dp),
    height: Dp = 44.dp,
    horizontalPadding: Dp = 8.dp,
) {
    Box(
        modifier
            .heightIn(min = height)
            .clip(shape)
            .background(if (selected) selectedContainer else IsleColors.SurfaceHigher)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) selectedContent else IsleColors.TextChip,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Nút chọn màu viền phát sáng chuẩn OriginOS 6 với hiệu ứng vòng chọn spring scale mượt mà.
 */
@Composable
fun ColorSwatchPill(
    label: String,
    swatchColor: Color?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAuto: Boolean = false,
) {
    val ringScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "swatchRingScale",
    )
    val accentRef = swatchColor ?: IsleColors.Accent
    val containerColor by animateColorAsState(
        targetValue = if (selected) IsleColors.SurfaceHigher else IsleColors.SurfaceHigh,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swatchContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentRef.copy(alpha = 0.72f) else IsleColors.BorderHairline,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swatchBorder",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swatchRingAlpha",
    )

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(
                width = if (selected) 1.2.dp else 0.8.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .scale(ringScale),
            contentAlignment = Alignment.Center,
        ) {
            if (ringAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .alpha(ringAlpha)
                        .border(1.8.dp, accentRef, CircleShape),
                )
            }
            when {
                isAuto -> {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF00BFA5),
                                        Color(0xFF0A59F7),
                                        Color(0xFF9D4EDD),
                                        Color(0xFFFFB020),
                                        Color(0xFF00BFA5),
                                    ),
                                ),
                            ),
                    )
                }
                swatchColor != null -> {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(swatchColor),
                    )
                }
                else -> {
                    val slashColor = IsleColors.TextSecondary
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(IsleColors.TextTertiary.copy(alpha = 0.25f))
                            .border(0.8.dp, IsleColors.BorderHairline, CircleShape),
                    ) {
                        Canvas(Modifier.matchParentSize()) {
                            drawLine(
                                color = slashColor,
                                start = Offset(size.width * 0.24f, size.height * 0.76f),
                                end = Offset(size.width * 0.76f, size.height * 0.24f),
                                strokeWidth = 1.6.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) IsleColors.Text else IsleColors.TextChip,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Centered grey message for empty lists. */
@Composable
fun EmptyMessage(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 48.dp),
        fontSize = 15.sp,
        color = IsleColors.TextSecondary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
