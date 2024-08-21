package org.dweb_browser.sys.window.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.window.helper.watchedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WindowContentScaffoldWrapper(
  renderScope: WindowContentRenderScope,
  modifier: Modifier = Modifier,
  scrollBehavior: TopAppBarScrollBehavior,
  topBar: @Composable () -> Unit = {},
  containerColor: Color? = null,
  contentColor: Color? = null,
  content: @Composable (PaddingValues) -> Unit,
) {
  Scaffold(
    modifier = modifier.withRenderScope(renderScope)
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    // TODO 添加 ime 的支持
    contentWindowInsets = WindowInsets(0),
    topBar = {
      topBar()
    },
    containerColor = containerColor ?: MaterialTheme.colorScheme.background,
    contentColor = contentColor ?: contentColorFor(MaterialTheme.colorScheme.background),
    content = { innerPadding ->
      content(innerPadding)
    },
  )
}

@Stable
fun Modifier.withRenderScope(renderScope: WindowContentRenderScope) = when (renderScope) {
  WindowContentRenderScope.Unspecified -> this
  else -> renderScope.run {
    this@withRenderScope.requiredSize(widthDp / scale, heightDp / scale).scale(scale)// 原始大小
  }
}

@Composable
fun WindowController.GoBackButton() {
  val uiScope = rememberCoroutineScope()
  val win = this
  val goBackButtonId = remember { randomUUID() }
  /// 我们允许 WindowContentScaffold 在一个 win 中多次使用，比方说响应式布局中，将多个路由页面同时展示
  /// 这时候我们会有多个 TopBar 在同时渲染，而为了让 GoBackButton 只出现一个
  /// 我们将 goBackButtonId 放到一个数组中，第一个id获得 GoBackButton 的渲染权
  /// 因此页面的渲染顺序很重要
  DisposableEffect(goBackButtonId) {
    win.navigation.goBackButtonStack.add(goBackButtonId)
    onDispose {
      win.navigation.goBackButtonStack.remove(goBackButtonId)
    }
  }

  val canGoBack by win.watchedState { canGoBack }
  if (canGoBack == true && win.navigation.goBackButtonStack.lastOrNull() == goBackButtonId) {
    IconButton(onClick = {
      // TODO 提供导航功能
      uiScope.launch { win.navigation.emitGoBack() }
    }) {
      Icon(
        imageVector = Icons.Default.ArrowBackIosNew,
        contentDescription = "Go Back",
      )
    }
  }
}


@Composable
fun WindowContentRenderScope.WindowSurface(
  modifier: Modifier = Modifier,
  tonalElevation: Dp = 0.dp,
  shadowElevation: Dp = 0.dp,
  color: Color? = null,
  contentColor: Color? = null,
  border: BorderStroke? = null,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = modifier.withRenderScope(this),
    tonalElevation = tonalElevation,
    shadowElevation = shadowElevation,
    color = color ?: MaterialTheme.colorScheme.background,
    contentColor = contentColor ?: contentColorFor(MaterialTheme.colorScheme.background),
    border = border,
    content = content,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowContentRenderScope.WindowContentScaffold(
  modifier: Modifier = Modifier,
  scrollBehavior: TopAppBarScrollBehavior,
  topBar: @Composable () -> Unit,
  containerColor: Color? = null,
  contentColor: Color? = null,
  content: @Composable (PaddingValues) -> Unit,
) {
  WindowContentScaffoldWrapper(
    this,
    modifier,
    scrollBehavior = scrollBehavior,
    topBar = topBar,
    containerColor = containerColor,
    contentColor = contentColor,
    content = content,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowContentRenderScope.WindowContentScaffoldWithTitle(
  modifier: Modifier = Modifier,
  topBarTitle: @Composable (scrollBehavior: TopAppBarScrollBehavior) -> Unit = {},
  topBarActions: @Composable RowScope.() -> Unit = {},
  containerColor: Color? = null,
  contentColor: Color? = null,
  content: @Composable (PaddingValues) -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  WindowContentScaffoldWrapper(
    this,
    modifier,
    scrollBehavior = scrollBehavior,
    topBar = {
      val win = LocalWindowController.current
      TopAppBar(
        title = { topBarTitle(scrollBehavior) },
        windowInsets = WindowInsets(0),
        actions = topBarActions,
        navigationIcon = { win.GoBackButton() },
        scrollBehavior = scrollBehavior,
      )
    },
    containerColor = containerColor,
    contentColor = contentColor,
    content = content,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowContentRenderScope.WindowContentScaffoldWithTitleText(
  modifier: Modifier = Modifier,
  topBarTitleText: String,
  topBarActions: @Composable RowScope.() -> Unit = {},
  containerColor: Color? = null,
  contentColor: Color? = null,
  content: @Composable (PaddingValues) -> Unit,
) {
  WindowContentScaffoldWithTitle(
    modifier,
    topBarTitle = {
      Text(topBarTitleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    },
    topBarActions = topBarActions,
    containerColor = containerColor,
    contentColor = contentColor,
    content = content,
  )
}