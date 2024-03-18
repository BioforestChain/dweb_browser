package org.dweb_browser.browser.web.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow

/**
 * 统一规划顶部工具栏的显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopBar(
  title: String,
  navigationIcon: @Composable () -> Unit = {},
  actions: (@Composable RowScope.() -> Unit) = {},
  scrollBehavior: TopAppBarScrollBehavior? = null
) {
  CenterAlignedTopAppBar(
    windowInsets = WindowInsets(0, 0, 0, 0), // 顶部
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      titleContentColor = MaterialTheme.colorScheme.primary,
    ),
    title = { Text(text = title, overflow = TextOverflow.Ellipsis) },
    navigationIcon = navigationIcon,
    actions = actions,
    scrollBehavior = scrollBehavior
  )
}

/**
 * 统一规划顶部工具栏的显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopBar(
  title: String,
  enableNavigation: Boolean = true,
  onNavigationBack: () -> Unit = {},
  actions: (@Composable RowScope.() -> Unit) = {},
  scrollBehavior: TopAppBarScrollBehavior? = null
) {
  CenterAlignedTopAppBar(
    windowInsets = WindowInsets(0, 0, 0, 0), // 顶部
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      titleContentColor = MaterialTheme.colorScheme.primary,
    ),
    title = { Text(text = title, overflow = TextOverflow.Ellipsis) },
    navigationIcon = {
      if (enableNavigation) {
        IconButton(onClick = onNavigationBack) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
            contentDescription = "Back to list"
          )
        }
      }
    },
    actions = actions,
    scrollBehavior = scrollBehavior
  )
}