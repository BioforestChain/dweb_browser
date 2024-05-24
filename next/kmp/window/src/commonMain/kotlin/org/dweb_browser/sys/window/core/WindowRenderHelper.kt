package org.dweb_browser.sys.window.core

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun WindowContentRenderScope.WindowContentScaffold(
  modifier: Modifier = Modifier,
  topBar: @Composable () -> Unit = {},
  content: @Composable (PaddingValues) -> Unit,
) {
  val windowRenderScope = this
//  val win = LocalWindowController.current
//  win.GoBackHandler {
//    win.hide()
//  }
  Scaffold(
    modifier = windowRenderScope.run {
      modifier
        .fillMaxSize()
        .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
        .scale(scale)
    },
    // TODO 添加 ime 的支持
    contentWindowInsets = WindowInsets(0),
    topBar = {
      topBar()
    },
  ) { innerPadding ->
    content(innerPadding)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowContentRenderScope.WindowContentScaffold(
  modifier: Modifier = Modifier,
  topBarTitle: String,
  content: @Composable (PaddingValues) -> Unit,
) {
  WindowContentScaffold(modifier, topBar = {
    val win = LocalWindowController.current
    val uiScope = rememberCoroutineScope()
    TopAppBar(
      title = {
        Text(
          topBarTitle,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      },
      windowInsets = WindowInsets(0),
      navigationIcon = {
        IconButton(onClick = {
          // TODO 提供导航功能
          uiScope.launch { win.hide() }
        }) {
          Icon(
            imageVector = Icons.Default.ArrowBackIosNew,
            contentDescription = "Go Back",
          )
        }
      },
    )
  }, content = content)
}