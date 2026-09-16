package com.skipskip.app

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.skipskip.app.ui.AppIconSize
import com.skipskip.app.ui.CardRadius
import com.skipskip.app.ui.CellDivider
import com.skipskip.app.ui.CompactSwitch
import com.skipskip.app.ui.GroupHeader
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
 * 「排除的应用」：开关打开后 SkipSkip 在该应用中不自动点击。
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
    onToggle: (packageName: String, excluded: Boolean) -> Unit,
) {
    // 组还没换时，先让开关在原地播完动画
    var checked by remember(app.packageName) { mutableStateOf(excluded) }
    SettingsCell(
        title = app.label,
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
        verticalPadding = 10.dp,
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
