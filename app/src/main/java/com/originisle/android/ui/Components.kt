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
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

/** Shared building blocks for OriginOS 6 UI: grouped squircle cards, settings rows, pills, switches. */

/**
 * OriginOS ROM 16.0 G2 continuous rounded corner shape (squircle).
 * Uses symmetric dual-cubic Bezier curves per corner for zero-curvature tangency at straight edges.
 */
class G2RoundedCornerShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
) : CornerBasedShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomEnd = bottomEnd,
    bottomStart = bottomStart,
) {
    constructor(cornerRadius: Dp) : this(
        topStart = CornerSize(cornerRadius),
        topEnd = CornerSize(cornerRadius),
        bottomEnd = CornerSize(cornerRadius),
        bottomStart = CornerSize(cornerRadius),
    )

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        if (topStart + topEnd + bottomEnd + bottomStart == 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }
        val tl = if (layoutDirection == LayoutDirection.Ltr) topStart else topEnd
        val tr = if (layoutDirection == LayoutDirection.Ltr) topEnd else topStart
        val br = if (layoutDirection == LayoutDirection.Ltr) bottomEnd else bottomStart
        val bl = if (layoutDirection == LayoutDirection.Ltr) bottomStart else bottomEnd

        val w = size.width
        val h = size.height
        val halfMin = min(w, h) * 0.5f

        fun span(r: Float): Float = (r * 1.28f).coerceAtMost(halfMin)

        val pTL = span(tl)
        val pTR = span(tr)
        val pBR = span(br)
        val pBL = span(bl)

        val kMid = 0.2288f
        val kHandle = 0.65f

        val path = Path().apply {
            moveTo(pTL, 0f)
            lineTo(w - pTR, 0f)
            if (pTR > 0f) {
                val d = pTR * kMid
                cubicTo(w - pTR * kHandle, 0f, w - 2f * d, 0f, w - d, d)
                cubicTo(w, 2f * d, w, pTR * kHandle, w, pTR)
            }
            lineTo(w, h - pBR)
            if (pBR > 0f) {
                val d = pBR * kMid
                cubicTo(w, h - pBR * kHandle, w, h - 2f * d, w - d, h - d)
                cubicTo(w - 2f * d, h, w - pBR * kHandle, h, w - pBR, h)
            }
            lineTo(pBL, h)
            if (pBL > 0f) {
                val d = pBL * kMid
                cubicTo(pBL * kHandle, h, 2f * d, h, d, h - d)
                cubicTo(0f, h - 2f * d, 0f, h - pBL * kHandle, 0f, h - pBL)
            }
            lineTo(0f, pTL)
            if (pTL > 0f) {
                val d = pTL * kMid
                cubicTo(0f, pTL * kHandle, 0f, 2f * d, d, d)
                cubicTo(2f * d, 0f, pTL * kHandle, 0f, pTL, 0f)
            }
            close()
        }
        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = G2RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is G2RoundedCornerShape) return false
        return topStart == other.topStart &&
            topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd &&
            bottomStart == other.bottomStart
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return result
    }
}

/** Page padding shared by every screen. */
val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 28.dp)

private val GroupRadius = 24.dp

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
    val cardShape = G2RoundedCornerShape(24.dp)

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
        shape = G2RoundedCornerShape(24.dp),
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

/** OriginOS 6-7 Liquid Glass card with 24dp G2 squircle corners and Edge Light border. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val cardShape = G2RoundedCornerShape(GroupRadius)
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
    return G2RoundedCornerShape(
        topStart = CornerSize(top),
        topEnd = CornerSize(top),
        bottomEnd = CornerSize(bottom),
        bottomStart = CornerSize(bottom),
    )
}

/** Hairline between rows in OriginOS 6, inset to line up with row titles. */
@Composable
fun GroupDivider(inset: Dp = 56.dp) {
    Box(Modifier.padding(start = inset).fillMaxWidth().height(0.5.dp).background(IsleColors.Divider))
}

/** OriginOS 6-7 squircle icon tile (13dp G2) with subtle static edge highlight. */
@Composable
fun IconTile(icon: ImageVector, color: Color = IsleColors.Accent, size: Dp = 38.dp) {
    val tileShape = G2RoundedCornerShape(13.dp)
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
                .clip(G2RoundedCornerShape(18.dp))
                .background(IsleColors.SurfaceHigh)
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                    shape = G2RoundedCornerShape(18.dp),
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
 * OriginOS ROM 16.0 MoveBoolButton Switch (44dp x 28dp):
 * - OFF: track #59CCCCCC (Light) / #75464646 (Dark), thumb vòng tròn rỗng (hollow ring) nét 2.5dp đường kính 18dp.
 * - ON: track accent alpha 0.24f (Light) / 0.36f (Dark), thumb tròn đặc trắng + halo mềm màu accent.
 */
@Composable
fun IsleSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val isDark = IsleColors.isDark
    val accent = IsleColors.Accent

    val targetTrackColor = if (checked) {
        accent.copy(alpha = if (isDark) 0.36f else 0.24f)
    } else {
        if (isDark) Color(0x75464646) else Color(0x59CCCCCC)
    }
    val trackColor by animateColorAsState(
        targetValue = targetTrackColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "isleSwitchTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 5.dp,
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
    val onProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "isleSwitchOnProgress",
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
            .size(width = 44.dp, height = 28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .then(toggleModifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .size(18.dp)
                .scale(thumbScale),
            contentAlignment = Alignment.Center,
        ) {
            // Soft accent halo when ON
            if (onProgress > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .alpha(onProgress * 0.45f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(accent.copy(alpha = 0.7f), Color.Transparent),
                            ),
                        ),
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (onProgress > 0.01f) {
                    drawCircle(
                        color = Color.White.copy(alpha = onProgress),
                        radius = size.minDimension * 0.5f,
                    )
                }
                if (onProgress < 0.99f) {
                    val strokePx = 2.5.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 1f - onProgress),
                        radius = (size.minDimension - strokePx) * 0.5f,
                        style = Stroke(width = strokePx),
                    )
                }
            }
        }
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

    val segmentTrackShape = G2RoundedCornerShape(18.dp)
    val segmentThumbShape = G2RoundedCornerShape(15.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(segmentTrackShape)
            .background(track)
            .border(
                width = 0.6.dp,
                brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                shape = segmentTrackShape,
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
                .clip(segmentThumbShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .border(
                    width = 0.6.dp,
                    brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                    shape = segmentThumbShape,
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
                        .clip(segmentThumbShape)
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
    shape: Shape = G2RoundedCornerShape(18.dp),
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
            .clip(G2RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(
                width = if (selected) 1.2.dp else 0.8.dp,
                color = borderColor,
                shape = G2RoundedCornerShape(14.dp),
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
 * Nền Space System 2.0 với quầng sáng Ambient Light đa điểm dừng ở đỉnh màn hình,
 * hòa sắc liền mạch lên Status Bar (超无界状态栏).
 */
@Composable
fun OriginSpaceBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val bgColor = IsleColors.Background
    val glowColor = IsleColors.AmbientGlow
    val isDark = IsleColors.isDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val topWashAlpha = if (isDark) 0.06f else 0.04f
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to glowColor.copy(alpha = (glowColor.alpha + topWashAlpha).coerceAtMost(0.24f)),
                        0.18f to glowColor.copy(alpha = glowColor.alpha * 0.45f),
                        0.36f to Color.Transparent,
                    ),
                    startY = 0f,
                    endY = (size.height * 0.36f).coerceAtLeast(1f),
                ),
            )
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to glowColor.copy(alpha = (glowColor.alpha * 1.35f).coerceAtMost(0.26f)),
                        0.32f to glowColor.copy(alpha = glowColor.alpha * 0.80f),
                        0.68f to glowColor.copy(alpha = glowColor.alpha * 0.25f),
                        1.0f to Color.Transparent,
                    ),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = (size.width * 1.05f).coerceAtLeast(1f),
                ),
            )
        }
        content()
    }
}

/** OriginOS ROM 16.0 VTipCard-style warning/tip banner with 12dp G2 corners. */
@Composable
fun WarningBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val bannerShape = G2RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(bannerShape)
            .background(IsleColors.Surface)
            .border(width = 0.8.dp, color = IsleColors.Divider, shape = bannerShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = IsleColors.Text,
            )
            Text(
                text = message,
                fontSize = 12.sp,
                color = IsleColors.TextSecondary,
                lineHeight = 17.sp,
            )
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF456FFF),
                modifier = Modifier
                    .clip(G2RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Tab item cho thanh điều hướng nổi OriginOS 6-7. */
data class OriginNavTab(
    val label: String,
    val icon: ImageVector,
)

/**
 * Thanh điều hướng kính lỏng nổi OriginOS 7 (Liquid Glass Dock + Liquid Lens)
 * với viền sáng Edge Light 2 lớp, inner specular top highlight (0.5.dp),
 * viên thuốc Liquid Lens trượt spring (0.72f) và phản hồi Touch Light (0.96f).
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
    val isDark = IsleColors.isDark
    val specularTop = if (isDark) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.92f)
    val lensSpecularTop = if (isDark) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.78f)
    val dockShape = G2RoundedCornerShape(32.dp)
    val lensShape = G2RoundedCornerShape(27.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = dockShape,
                    ambientColor = Color.Black.copy(alpha = 0.16f),
                    spotColor = Color.Black.copy(alpha = 0.22f),
                )
                .clip(dockShape)
                .background(IsleColors.FloatingGlassBg)
                .border(
                    width = 0.9.dp,
                    brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                    shape = dockShape,
                ),
        ) {
            // Inner specular top highlight (0.5.dp) for two-layer Edge Light refraction
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(0.9.dp)
                    .border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to specularTop,
                                0.42f to Color.Transparent,
                                1.0f to Color.Transparent,
                            ),
                        ),
                        shape = dockShape,
                    ),
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .selectableGroup(),
            ) {
                val tabWidth = maxWidth / tabs.size
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * safeSelected,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "originNavIndicator",
                )

                // Liquid Lens sliding pill indicator
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(lensShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    IsleColors.Accent.copy(alpha = 0.22f),
                                    IsleColors.Accent.copy(alpha = 0.12f),
                                ),
                            ),
                        )
                        .border(
                            width = 0.7.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    IsleColors.Accent.copy(alpha = 0.36f),
                                    IsleColors.Accent.copy(alpha = 0.08f),
                                ),
                            ),
                            shape = lensShape,
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(0.7.dp)
                            .border(
                                width = 0.5.dp,
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to lensSpecularTop,
                                        0.48f to Color.Transparent,
                                        1.0f to Color.Transparent,
                                    ),
                                ),
                                shape = lensShape,
                            ),
                    )
                }

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
                        val touchLightAlpha by animateFloatAsState(
                            targetValue = if (pressed) 0.10f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "navTabTouchLight_$index",
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
                                .clip(lensShape)
                                .background(IsleColors.Accent.copy(alpha = touchLightAlpha))
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
}
