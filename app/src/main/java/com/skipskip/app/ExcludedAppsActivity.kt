package com.skipskip.app

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.drawable.toBitmap
import com.skipskip.app.ui.AppIconSize
import com.skipskip.app.ui.CardRadius
import com.skipskip.app.ui.CellDivider
import com.skipskip.app.ui.CompactSwitch
import com.skipskip.app.ui.GroupHeader
import com.skipskip.app.ui.HeaderBottomPadding
import com.skipskip.app.ui.HeaderInset
import com.skipskip.app.ui.PageHeader
import com.skipskip.app.ui.ScreenPadding
import com.skipskip.app.ui.SettingsCell
import com.skipskip.app.ui.SettingsPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/**
 * 「排除的应用」：开关打开后 SkipSkip 在该应用中不自动跳过。
 */
class ExcludedAppsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsPage { innerPadding ->
                ExcludedAppsRoute(
                    onBack = { finish() },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

private data class AppEntry(
    val packageName: String,
    val label: String,
)

@Composable
private fun ExcludedAppsRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val apps by produceState<List<AppEntry>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadLauncherApps(context.packageManager, context.packageName) }
    }
    var excluded by remember { mutableStateOf(SkipPrefs.excludedPackages(context)) }
    var showPackageNames by remember { mutableStateOf(SkipPrefs.showPackageNames(context)) }
    var query by remember { mutableStateOf("") }
    val filtered by remember {
        derivedStateOf {
            val all = apps ?: return@derivedStateOf emptyList()
            val q = query.trim()
            if (q.isEmpty()) all
            else all.filter {
                it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
            }
        }
    }
    val onList = remember(filtered, excluded) { filtered.filter { it.packageName in excluded } }
    val offList = remember(filtered, excluded) { filtered.filter { it.packageName !in excluded } }
    val scope = rememberCoroutineScope()
    val commitToggle: (String, Boolean) -> Unit = { pkg, isOn ->
        SkipPrefs.setExcluded(context, pkg, isOn)
        scope.launch {
            delay(SwitchMoveDelayMs)
            excluded = SkipPrefs.excludedPackages(context)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PageHeader(
            title = stringResource(R.string.excluded_apps_title),
            onBack = onBack,
            actions = {
                OverflowMenu(
                    showPackageNames = showPackageNames,
                    onShowPackageNamesChange = { show ->
                        SkipPrefs.setShowPackageNames(context, show)
                        showPackageNames = show
                    },
                )
            },
        )
        Box(modifier = Modifier.padding(bottom = 4.dp)) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "footer") {
                Text(
                    text = stringResource(R.string.excluded_apps_footer),
                    modifier = Modifier.padding(
                        start = HeaderInset + ScreenPadding,
                        end = HeaderInset + ScreenPadding,
                        top = 12.dp,
                        bottom = 4.dp,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (apps == null) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filtered.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.excluded_apps_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenPadding, vertical = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                if (onList.isNotEmpty()) {
                    item(key = "header_on") {
                        Box(modifier = Modifier.padding(horizontal = ScreenPadding)) {
                            GroupHeader(stringResource(R.string.excluded_apps_group_on, onList.size))
                        }
                    }
                    appRows(
                        apps = onList,
                        excluded = true,
                        showPackageNames = showPackageNames,
                        onToggle = commitToggle,
                    )
                }
                if (offList.isNotEmpty()) {
                    item(key = "header_off") {
                        Box(modifier = Modifier.padding(horizontal = ScreenPadding)) {
                            GroupHeader(stringResource(R.string.excluded_apps_group_off, offList.size))
                        }
                    }
                    appRows(
                        apps = offList,
                        excluded = false,
                        showPackageNames = showPackageNames,
                        onToggle = commitToggle,
                    )
                }
                item(key = "bottom") {
                    Box(modifier = Modifier.padding(bottom = ScreenPadding))
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(
    showPackageNames: Boolean,
    onShowPackageNamesChange: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // 横向：菜单右边贴三点图形右边缘（扣掉 48dp 热区两侧留白）
    // 纵向：菜单顶部贴 header 底边（按钮底 + header 底部留白）
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }
    var iconSize by remember { mutableStateOf(IntSize.Zero) }
    val headerBottomPx = with(LocalDensity.current) { HeaderBottomPadding.roundToPx() }
    val menuOffset = IntOffset(
        x = -(buttonSize.width - iconSize.width) / 2,
        y = buttonSize.height + headerBottomPx,
    )
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = expanded

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.onSizeChanged { buttonSize = it },
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.more),
                modifier = Modifier.onSizeChanged { iconSize = it },
            )
        }
        if (visibleState.currentState || visibleState.targetState) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = menuOffset,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(tween(120)) + scaleIn(
                        animationSpec = tween(140),
                        initialScale = 0.88f,
                        transformOrigin = MenuOrigin,
                    ),
                    exit = fadeOut(tween(90)) + scaleOut(
                        animationSpec = tween(90),
                        targetScale = 0.92f,
                        transformOrigin = MenuOrigin,
                    ),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        tonalElevation = 0.dp,
                        shadowElevation = 3.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    onShowPackageNamesChange(!showPackageNames)
                                    expanded = false
                                }
                                .padding(horizontal = 16.dp)
                                .height(40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(
                                    if (showPackageNames) R.string.hide_package_name
                                    else R.string.show_package_name,
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding)
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.excluded_apps_search),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                inner()
            },
        )
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = stringResource(R.string.clear),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun LazyListScope.appRows(
    apps: List<AppEntry>,
    excluded: Boolean,
    showPackageNames: Boolean,
    onToggle: (packageName: String, excluded: Boolean) -> Unit,
) {
    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
        val isFirst = index == 0
        val isLast = index == apps.lastIndex
        val shape = RoundedCornerShape(
            topStart = if (isFirst) CardRadius else 0.dp,
            topEnd = if (isFirst) CardRadius else 0.dp,
            bottomStart = if (isLast) CardRadius else 0.dp,
            bottomEnd = if (isLast) CardRadius else 0.dp,
        )
        Column(
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            AppSwitchRow(
                app = app,
                excluded = excluded,
                showPackageName = showPackageNames,
                onToggle = onToggle,
            )
            if (!isLast) CellDivider()
        }
    }
}

@Composable
private fun AppSwitchRow(
    app: AppEntry,
    excluded: Boolean,
    showPackageName: Boolean,
    onToggle: (packageName: String, excluded: Boolean) -> Unit,
) {
    // 组还没换时，先让开关在原地播完动画
    var checked by remember(app.packageName) { mutableStateOf(excluded) }
    SettingsCell(
        title = app.label,
        summary = if (showPackageName) app.packageName else null,
        summaryMaxLines = 1,
        titleSummarySpacing = 0.dp,
        onClick = {
            val next = !checked
            checked = next
            onToggle(app.packageName, next)
        },
        leading = { AppIcon(app.packageName) },
        trailing = {
            CompactSwitch(
                checked = checked,
                onCheckedChange = { on ->
                    checked = on
                    onToggle(app.packageName, on)
                },
            )
        },
        verticalPadding = 8.dp,
    )
}

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
                drawable.toBitmap(width = IconPx, height = IconPx).asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .size(AppIconSize)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

private const val IconPx = 144
private const val SwitchMoveDelayMs = 300L

/** 菜单从三点那一侧展开 */
private val MenuOrigin = TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0f)

private fun loadLauncherApps(pm: PackageManager, selfPackage: String): List<AppEntry> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }
    val collator = Collator.getInstance(Locale.CHINA)
    return resolved
        .asSequence()
        .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
        .filter { (pkg, _) -> pkg != selfPackage }
        .distinctBy { (pkg, _) -> pkg }
        .map { (pkg, label) -> AppEntry(pkg, label) }
        .sortedWith(compareBy(collator) { it.label })
        .toList()
}
