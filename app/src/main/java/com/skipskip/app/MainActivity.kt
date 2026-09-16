package com.skipskip.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skipskip.app.ui.theme.SkipSkipTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkipSkipTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    // 页面底用浅灰，让白色卡片浮出来，与系统设置页一致
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) { innerPadding ->
                    HomeRoute(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun HomeRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var serviceEnabled by remember { mutableStateOf(false) }
    var autoClickEnabled by remember { mutableStateOf(true) }
    var batteryIgnored by remember { mutableStateOf(false) }

    fun refresh() {
        serviceEnabled = SkipSkipAccessibilityService.isEnabled(context)
        autoClickEnabled = SkipPrefs.isAutoClickEnabled(context)
        batteryIgnored = BatteryHelper.isIgnoringOptimizations(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val preferencesListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            refresh()
        }
        SkipPrefs.registerListener(context, preferencesListener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            SkipPrefs.unregisterListener(context, preferencesListener)
        }
    }

    HomeScreen(
        serviceEnabled = serviceEnabled,
        autoClickEnabled = autoClickEnabled,
        batteryIgnored = batteryIgnored,
        onOpenAccessibility = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        onAutoClickChange = { enabled ->
            SkipPrefs.setAutoClickEnabled(context, enabled)
            autoClickEnabled = enabled
        },
        onRequestBattery = {
            runCatching {
                context.startActivity(BatteryHelper.createIgnoreOptimizationsIntent(context))
            }.onFailure {
                context.startActivity(BatteryHelper.createBatterySettingsIntent())
            }
        },
        onOpenRecents = { SkipSkipAccessibilityService.openRecents() },
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    serviceEnabled: Boolean,
    autoClickEnabled: Boolean,
    batteryIgnored: Boolean,
    onOpenAccessibility: () -> Unit,
    onAutoClickChange: (Boolean) -> Unit,
    onRequestBattery: () -> Unit,
    onOpenRecents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(bottom = ScreenPadding),
    ) {
        // 页面标题：与系统设置页一致，常规字重、左对齐
        Text(
            text = stringResource(R.string.home_title),
            modifier = Modifier.padding(
                start = HeaderInset,
                top = 12.dp,
                bottom = 20.dp,
            ),
            style = MaterialTheme.typography.titleLarge,
        )

        // 第一组：状态（无小标题）
        SettingsGroup {
            StatusCell(
                serviceEnabled = serviceEnabled,
                autoClickEnabled = autoClickEnabled,
                onAutoClickChange = onAutoClickChange,
                onOpenAccessibility = onOpenAccessibility,
            )
        }
        if (!serviceEnabled) {
            GroupFooter(stringResource(R.string.accessibility_guide))
        }

        // 第二组：建议
        GroupHeader(stringResource(R.string.section_settings))
        SettingsGroup {
            if (!batteryIgnored) {
                SettingsCell(
                    title = stringResource(R.string.battery_title),
                    summary = stringResource(R.string.battery_desc_off),
                    onClick = onRequestBattery,
                    trailing = { Chevron() },
                )
                CellDivider()
            }
            // 已授权时可借无障碍全局动作跳到最近任务；加锁本身仍需用户手动
            SettingsCell(
                title = stringResource(R.string.lock_hint_title),
                summary = stringResource(
                    if (serviceEnabled) R.string.lock_hint_desc_action else R.string.lock_hint_desc,
                ),
                onClick = if (serviceEnabled) onOpenRecents else null,
                trailing = if (serviceEnabled) ({ Chevron() }) else null,
            )
        }
    }
}

// 尺寸刻度：参考系统设置页
private val ScreenPadding = 12.dp     // 卡片到屏幕边
private val CellPadding = 16.dp       // 卡片内文字到卡片边
private val HeaderInset = 16.dp       // 小标题 / 页脚缩进，与卡片内文字对齐
private val CardRadius = 14.dp
private const val SwitchScale = 0.85f
private val GroupGap = 20.dp          // 组与组之间

/** 白色圆角卡片，内部叠多个 cell */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(content = content)
    }
}

/** 组标题：小字、灰色、略缩进，位于卡片上方 */
@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = HeaderInset,
            end = HeaderInset,
            top = GroupGap,
            bottom = 8.dp,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 组页脚：卡片下方的一行灰色说明 */
@Composable
private fun GroupFooter(text: String) {
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
private fun CellDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = CellPadding),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * 通用一行：标题 + 可选说明 + 右侧控件；整行可点。
 */
@Composable
private fun SettingsCell(
    title: String,
    summary: String? = null,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = CellPadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
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
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun StatusCell(
    serviceEnabled: Boolean,
    autoClickEnabled: Boolean,
    onAutoClickChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    val statusColor = when {
        !serviceEnabled -> Color.Unspecified
        !autoClickEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    val statusText = when {
        !serviceEnabled -> stringResource(R.string.status_off)
        !autoClickEnabled -> stringResource(R.string.status_paused)
        else -> stringResource(R.string.status_on)
    }
    SettingsCell(
        title = statusText,
        titleColor = statusColor,
        onClick = if (serviceEnabled) null else onOpenAccessibility,
        trailing = {
            if (serviceEnabled) {
                // M3 开关无尺寸参数，整体缩放到接近厂商系统开关的观感
                Switch(
                    checked = autoClickEnabled,
                    onCheckedChange = onAutoClickChange,
                    modifier = Modifier.scale(SwitchScale),
                )
            } else {
                Chevron()
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SkipSkipTheme {
        HomeScreen(
            serviceEnabled = false,
            autoClickEnabled = true,
            batteryIgnored = false,
            onOpenAccessibility = {},
            onAutoClickChange = {},
            onRequestBattery = {},
            onOpenRecents = {},
        )
    }
}
