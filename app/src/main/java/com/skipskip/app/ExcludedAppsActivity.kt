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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.skipskip.app.ui.CardRadius
import com.skipskip.app.ui.CellDivider
import com.skipskip.app.ui.GroupFooter
import com.skipskip.app.ui.ScreenPadding
import com.skipskip.app.ui.SettingsCell
import com.skipskip.app.ui.theme.SkipSkipTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/**
 * 「排除的应用」：勾选后 SkipSkip 在该应用中不自动点击。
 */
class ExcludedAppsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkipSkipTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) { innerPadding ->
                    ExcludedAppsRoute(
                        onBack = { finish() },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
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

    // 只加载一次：带启动图标的应用，按中文排序，排除自己
    val apps by produceState<List<AppEntry>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadLauncherApps(context.packageManager, context.packageName) }
    }
    var excluded by remember { mutableStateOf(SkipPrefs.excludedPackages(context)) }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶栏：返回 + 标题，与系统设置二级页一致
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Text(
                text = stringResource(R.string.excluded_apps_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        val list = apps
        if (list == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ScreenPadding),
        ) {
            item {
                GroupFooter(stringResource(R.string.excluded_apps_footer))
                Box(modifier = Modifier.padding(top = 8.dp))
            }
            itemsIndexed(list, key = { _, app -> app.packageName }) { index, app ->
                val isFirst = index == 0
                val isLast = index == list.lastIndex
                // 整列表看起来是一张卡：首尾各自带圆角，中间用分割线
                val shape = RoundedCornerShape(
                    topStart = if (isFirst) CardRadius else 0.dp,
                    topEnd = if (isFirst) CardRadius else 0.dp,
                    bottomStart = if (isLast) CardRadius else 0.dp,
                    bottomEnd = if (isLast) CardRadius else 0.dp,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                ) {
                    val checked = app.packageName in excluded
                    val toggle = {
                        SkipPrefs.setExcluded(context, app.packageName, !checked)
                        excluded = SkipPrefs.excludedPackages(context)
                    }
                    SettingsCell(
                        title = app.label,
                        summary = app.packageName,
                        onClick = toggle,
                        leading = { AppIcon(app.packageName) },
                        trailing = {
                            Checkbox(checked = checked, onCheckedChange = { toggle() })
                        },
                    )
                    if (!isLast) CellDivider()
                }
            }
            item { Box(modifier = Modifier.padding(bottom = ScreenPadding)) }
        }
    }
}

/** 图标按需在 IO 线程加载，避免列表卡顿 */
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
    Box(modifier = Modifier.size(36.dp)) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

private const val IconPx = 144

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
