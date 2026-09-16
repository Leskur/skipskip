package com.skipskip.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * 系统设置页风格的分组列表组件。
 * 尺寸刻度参考 ColorOS / 厂商设置页：卡片贴边 12、卡片内 16、行高略收。
 */

val ScreenPadding = 12.dp     // 卡片到屏幕边
val CellPadding = 16.dp       // 卡片内文字到卡片边
val HeaderInset = 16.dp       // 小标题 / 页脚缩进，与卡片内文字对齐
val CardRadius = 14.dp
val GroupGap = 20.dp          // 组与组之间
val AppIconSize = 32.dp       // 与系统设置列表图标接近
val CellVerticalPadding = 12.dp // 首页单行；有副标题时仍够用
const val SwitchScale = 0.85f // M3 开关无尺寸参数，整体缩放到接近厂商观感
private val SwitchTrackWidth = 52.dp
private val SwitchTrackHeight = 32.dp
private val ChevronSize = 18.dp

/** 缩放开关，并收掉 scale 留下的空白，让它和 32dp 图标、正文垂直对齐 */
@Composable
fun CompactSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    Box(
        modifier = Modifier.size(
            width = SwitchTrackWidth * SwitchScale,
            height = SwitchTrackHeight * SwitchScale,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(SwitchScale),
            colors = SwitchDefaults.colors(
                // 关闭态用浅轨道，避免 M3 默认灰显得过重
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** 白色圆角卡片，内部叠多个 cell；显式 clip，让首尾行涟漪也吃到圆角 */
@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(CardRadius)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(content = content)
    }
}

/** 组标题：小字、灰色、略缩进，位于卡片上方 */
@Composable
fun GroupHeader(
    text: String,
    topPadding: Dp = GroupGap,
) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = HeaderInset,
            end = HeaderInset,
            top = topPadding,
            bottom = 8.dp,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 组页脚：卡片下方的一行灰色说明 */
@Composable
fun GroupFooter(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = HeaderInset,
            end = HeaderInset,
            top = 8.dp,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun CellDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = CellPadding),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

/** ColorOS 风格：细、浅、略小的右箭头 */
@Composable
fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(ChevronSize),
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
    )
}

/**
 * 通用一行：可选左侧内容 + 标题 + 可选说明 + 右侧控件；整行可点。
 * 涟漪用淡灰、bounded，由外层卡片 clip 吃掉圆角外溢。
 */
@Composable
fun SettingsCell(
    title: String,
    summary: String? = null,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    verticalPadding: Dp = CellVerticalPadding,
    summaryMaxLines: Int = Int.MAX_VALUE,
    titleSummarySpacing: Dp = 1.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(
                            bounded = true,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        ),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = CellPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(titleSummarySpacing),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = summaryMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}
