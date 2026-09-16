package com.skipskip.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skipskip.app.R
import com.skipskip.app.ui.theme.SkipSkipTheme

/** 设置页公共壳：浅灰底，让白色卡片浮出。 */
@Composable
fun SettingsPage(
    content: @Composable (PaddingValues) -> Unit,
) {
    SkipSkipTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            content = content,
        )
    }
}

/** 标题栏底部留白；弹出菜单据此贴到 header 底边 */
val HeaderBottomPadding = 8.dp

/** 与 Material IconButton 默认热区一致，保证首页/内页标题栏同高 */
private val HeaderActionMinHeight = 48.dp

/**
 * 页面标题栏（首页与内页同一套高度）。
 * [onBack]：二级页返回；[actions]：右上角操作（如三点菜单）。
 */
@Composable
fun PageHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (onBack != null) 4.dp else HeaderInset,
                end = if (actions != null) 4.dp else 16.dp,
                top = 4.dp,
                bottom = HeaderBottomPadding,
            )
            // 首页没有返回键时也占满与 IconButton 相同的高度，避免标题上下位置漂移
            .heightIn(min = HeaderActionMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions?.invoke()
    }
}
