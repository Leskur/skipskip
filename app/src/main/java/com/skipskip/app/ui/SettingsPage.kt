package com.skipskip.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

/**
 * 页面标题。带 [onBack] 时显示返回按钮（二级页）；
 * 不带时与首页标题对齐。
 */
@Composable
fun PageHeader(
    title: String,
    onBack: (() -> Unit)? = null,
) {
    if (onBack != null) {
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    } else {
        Text(
            text = title,
            modifier = Modifier.padding(
                start = HeaderInset,
                top = 12.dp,
                bottom = 20.dp,
            ),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
