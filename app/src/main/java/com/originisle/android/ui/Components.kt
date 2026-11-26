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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Shared building blocks for OriginOS 6 UI: grouped squircle cards, settings rows, pills, switches. */

/** Page padding shared by every screen. */
val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 28.dp)

private val GroupRadius = 26.dp

/** Top-level tab header with large Material 3 title and optional trailing action. */
@Composable
fun MainScreenHeader(title: String, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = IsleColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** Compact OriginOS 6-7 Atomic Widget shortcut card used in 2-column quick action rows. */
@Composable
fun TonalShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tileColor: Color = IsleColors.Accent,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "shortcutTouchScale",
    )
    val cardShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .scale(scale)
            .clip(cardShape)
            .background(IsleColors.Surface)
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                shape = cardShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconTile(icon = icon, color = tileColor, size = 36.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = IsleColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = IsleColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

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

/** A full-width (or weighted) rounded button with an optional leading icon and Touch Light spring. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSize: Dp = 18.dp,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pillButtonScale",
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(height / 2),
        color = container,
        contentColor = content,
        border = if (enabled) {
            BorderStroke(
                width = 0.7.dp,
                brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
            )
        } else {
            null
        },
        modifier = modifier
            .scale(scale)
            .heightIn(min = height)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics { role = Role.Button },
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
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

/** OriginOS 6-7 Liquid Glass card with 26dp squircle corners and Edge Light border. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val cardShape = RoundedCornerShape(GroupRadius)
    Column(
        modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(IsleColors.Surface)
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                shape = cardShape,
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
    Box(Modifier.padding(start = inset).fillMaxWidth().height(0.5.dp).background(IsleColors.Divider))
}

/** OriginOS 6-7 squircle icon tile (13dp) with subtle static edge highlight. */
@Composable
fun IconTile(icon: ImageVector, color: Color = IsleColors.Accent, size: Dp = 38.dp) {
    val tileShape = RoundedCornerShape(13.dp)
    Box(
        Modifier
            .size(size)
            .clip(tileShape)
            .background(color.copy(alpha = 0.14f))
            .border(
                width = 0.6.dp,
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.06f)),
                ),
                shape = tileShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = color)
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
        if (value != null) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = IsleColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 136.dp).padding(end = 4.dp),
            )
        }
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
                .clip(RoundedCornerShape(18.dp))
                .background(IsleColors.SurfaceHigh)
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                    shape = RoundedCornerShape(18.dp),
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
 * OriginOS 6-7 Pill Switch (50dp x 28dp) with First-frame Interruptible spring thumb physics.
 */
@Composable
fun IsleSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else IsleColors.SwitchOff,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "isleSwitchTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 25.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "isleSwitchThumbOffset",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (pressed) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "isleSwitchThumbScale",
    )

    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            indication = null,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(width = 50.dp, height = 28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(
                width = 0.6.dp,
                brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                shape = CircleShape,
            )
            .then(toggleModifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .size(22.dp)
                .scale(thumbScale)
                .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** OriginOS 6-7 Segmented Pill control with Liquid Motion sliding indicator. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    track: Color = IsleColors.SurfaceHigh,
    height: Dp = 46.dp,
) {
    if (options.isEmpty()) return
    val safeSelected = selected.coerceIn(0, options.lastIndex)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track)
            .border(
                width = 0.6.dp,
                brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                shape = CircleShape,
            )
            .padding(3.dp)
            .selectableGroup(),
    ) {
        val segmentWidth = maxWidth / options.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * safeSelected,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "segmentedIndicator",
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .border(
                    width = 0.6.dp,
                    brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                    shape = CircleShape,
                ),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val on = i == safeSelected
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.97f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "segmentScale_$i",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .scale(scale)
                        .clip(CircleShape)
                        .selectable(
                            selected = on,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(i) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) MaterialTheme.colorScheme.onSecondaryContainer else IsleColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** OriginOS 6-7 Squircle ChoiceChip with Touch Light spring feedback. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedContent: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    shape: Shape = RoundedCornerShape(14.dp),
    height: Dp = 44.dp,
    horizontalPadding: Dp = 8.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "choiceChipScale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) selectedContainer else IsleColors.SurfaceHigher,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "choiceChipBg",
    )

    Box(
        modifier
            .scale(scale)
            .heightIn(min = height)
            .clip(shape)
            .background(bgColor)
            .border(
                width = if (selected) 0.9.dp else 0.6.dp,
                brush = if (selected) {
                    Brush.verticalGradient(listOf(selectedContent.copy(alpha = 0.28f), selectedContent.copy(alpha = 0.08f)))
                } else {
                    Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom))
                },
                shape = shape,
            )
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) selectedContent else IsleColors.TextSecondary,
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

/**
 * Nền Space System 2.0 với quầng sáng Ambient Glow dịu ở đỉnh màn hình,
 * hòa sắc liền mạch lên Status Bar (超无界状态栏).
 */
@Composable
fun OriginSpaceBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val bgColor = IsleColors.Background
    val glowColor = IsleColors.AmbientGlow

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = (size.width * 0.95f).coerceAtLeast(1f),
                ),
            )
        }
        content()
    }
}

/** Tab item cho thanh điều hướng nổi OriginOS 6-7. */
data class OriginNavTab(
    val label: String,
    val icon: ImageVector,
)

/**
 * Thanh điều hướng kính lỏng nổi (Liquid Glass Floating Capsule Navigation Bar)
 * với chỉ báo trượt spring (Liquid Motion) và viền sáng Edge Light.
 */
@Composable
fun OriginFloatingNavBar(
    tabs: List<OriginNavTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    val safeSelected = selectedIndex.coerceIn(0, tabs.lastIndex)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.16f),
                    spotColor = Color.Black.copy(alpha = 0.22f),
                )
                .clip(CircleShape)
                .background(IsleColors.FloatingGlassBg)
                .border(
                    width = 0.9.dp,
                    brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                    shape = CircleShape,
                )
                .padding(5.dp)
                .selectableGroup(),
        ) {
            val tabWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * safeSelected,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "originNavIndicator",
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(IsleColors.Accent.copy(alpha = 0.16f))
                    .border(
                        width = 0.7.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                IsleColors.Accent.copy(alpha = 0.34f),
                                IsleColors.Accent.copy(alpha = 0.08f),
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )

            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == safeSelected
                    val interactionSource = remember { MutableInteractionSource() }
                    val pressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (pressed) 0.96f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "navTabScale_$index",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) IsleColors.Accent else IsleColors.TextSecondary,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "navTabColor_$index",
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .scale(scale)
                            .clip(CircleShape)
                            .selectable(
                                selected = selected,
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelect(index) },
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
