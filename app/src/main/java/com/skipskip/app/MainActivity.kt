package com.skipskip.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skipskip.app.ui.CellDivider
import com.skipskip.app.ui.Chevron
import com.skipskip.app.ui.CompactSwitch
import com.skipskip.app.ui.GroupFooter
import com.skipskip.app.ui.GroupHeader
import com.skipskip.app.ui.PageHeader
import com.skipskip.app.ui.ScreenPadding
import com.skipskip.app.ui.SettingsCell
import com.skipskip.app.ui.SettingsGroup
import com.skipskip.app.ui.SettingsPage
import com.skipskip.app.ui.theme.SkipSkipTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsPage { innerPadding ->
                HomeRoute(modifier = Modifier.padding(innerPadding))
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
    var excludedCount by remember { mutableIntStateOf(0) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateChecker.Result?>(null) }
    val scope = rememberCoroutineScope()
    val currentVersion = BuildConfig.VERSION_NAME

    fun refresh() {
        serviceEnabled = SkipSkipAccessibilityService.isEnabled(context)
        autoClickEnabled = SkipPrefs.isAutoClickEnabled(context)
        batteryIgnored = BatteryHelper.isIgnoringOptimizations(context)
        excludedCount = SkipPrefs.excludedPackages(context).size
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
        excludedCount = excludedCount,
        onOpenExcludedApps = {
            context.startActivity(Intent(context, ExcludedAppsActivity::class.java))
        },
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
        currentVersion = currentVersion,
        checkingUpdate = checkingUpdate,
        onCheckUpdate = {
            if (!checkingUpdate) {
                checkingUpdate = true
                updateResult = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { UpdateChecker.check(currentVersion) }
                    checkingUpdate = false
                    updateResult = result
                }
            }
        },
        modifier = modifier,
    )

    updateResult?.let { result ->
        UpdateResultDialog(
            result = result,
            onDismiss = { updateResult = null },
            onOpenPage = { url ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                updateResult = null
            },
        )
    }
}

@Composable
fun HomeScreen(
    serviceEnabled: Boolean,
    autoClickEnabled: Boolean,
    batteryIgnored: Boolean,
    excludedCount: Int,
    onOpenExcludedApps: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onAutoClickChange: (Boolean) -> Unit,
    onRequestBattery: () -> Unit,
    onOpenRecents: () -> Unit,
    currentVersion: String,
    checkingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PageHeader(title = stringResource(R.string.app_name))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding)
                .padding(bottom = ScreenPadding),
        ) {
            // 第一组：自动跳过
            GroupHeader(
                text = stringResource(R.string.section_auto_skip),
                topPadding = 4.dp,
            )
            SettingsGroup {
                StatusCell(
                    serviceEnabled = serviceEnabled,
                    autoClickEnabled = autoClickEnabled,
                    onAutoClickChange = onAutoClickChange,
                    onOpenAccessibility = onOpenAccessibility,
                )
                CellDivider()
                SettingsCell(
                    title = stringResource(R.string.excluded_apps_title),
                    summary = if (excludedCount == 0) {
                        stringResource(R.string.excluded_apps_summary_none)
                    } else {
                        stringResource(R.string.excluded_apps_summary, excludedCount)
                    },
                    onClick = onOpenExcludedApps,
                    trailing = { Chevron() },
                )
            }
            if (!serviceEnabled) {
                GroupFooter(
                    stringResource(R.string.accessibility_guide, stringResource(R.string.app_name)),
                )
            }

            // 第二组：保持运行
            GroupHeader(stringResource(R.string.section_keep_running))
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
                        stringResource(R.string.app_name),
                    ),
                    onClick = if (serviceEnabled) onOpenRecents else null,
                    trailing = if (serviceEnabled) ({ Chevron() }) else null,
                )
            }

            // 第三组：关于
            GroupHeader(stringResource(R.string.section_about))
            SettingsGroup {
                SettingsCell(
                    title = stringResource(R.string.update_title),
                    summary = if (checkingUpdate) {
                        stringResource(R.string.update_checking)
                    } else {
                        stringResource(R.string.update_summary, currentVersion)
                    },
                    onClick = if (checkingUpdate) null else onCheckUpdate,
                    trailing = { Chevron() },
                )
            }
        }
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
                CompactSwitch(
                    checked = autoClickEnabled,
                    onCheckedChange = onAutoClickChange,
                )
            } else {
                Chevron()
            }
        },
    )
}

@Composable
private fun UpdateResultDialog(
    result: UpdateChecker.Result,
    onDismiss: () -> Unit,
    onOpenPage: (String) -> Unit,
) {
    val title = stringResource(R.string.update_title)
    val (message, primary, primaryAction) = when (result) {
        is UpdateChecker.Result.Latest -> Triple(
            stringResource(R.string.update_latest),
            stringResource(R.string.ok),
            onDismiss,
        )
        is UpdateChecker.Result.Available -> Triple(
            stringResource(R.string.update_available, result.latest),
            stringResource(R.string.update_view),
            { onOpenPage(result.pageUrl) },
        )
        UpdateChecker.Result.Failed -> Triple(
            stringResource(R.string.update_failed),
            stringResource(R.string.update_view),
            { onOpenPage(UpdateChecker.RELEASES_PAGE) },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = primaryAction) {
                Text(primary)
            }
        },
        dismissButton = if (result is UpdateChecker.Result.Latest) {
            null
        } else {
            {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = if (result is UpdateChecker.Result.Available) {
                            stringResource(R.string.update_later)
                        } else {
                            stringResource(R.string.ok)
                        },
                    )
                }
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
            excludedCount = 0,
            onOpenExcludedApps = {},
            onOpenAccessibility = {},
            onAutoClickChange = {},
            onRequestBattery = {},
            onOpenRecents = {},
            currentVersion = "0.0.1",
            checkingUpdate = false,
            onCheckUpdate = {},
        )
    }
}
