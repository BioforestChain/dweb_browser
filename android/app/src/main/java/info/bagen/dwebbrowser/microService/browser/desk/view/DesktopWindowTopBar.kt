package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.desk.DesktopWindowController
import info.bagen.dwebbrowser.microService.core.WindowState
import kotlinx.coroutines.launch

@Composable
internal fun WindowTopBar(
  modifier: Modifier,
  winEdge: WindowEdge,
  winState: WindowState,
  win: DesktopWindowController
) {
  val density = LocalDensity.current;
  val coroutineScope = rememberCoroutineScope()
  Box(
    modifier = modifier
      .background(MaterialTheme.colorScheme.onPrimaryContainer)
      .fillMaxWidth()
      .height(winEdge.top.dp)
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color.Black.copy(alpha = 0.2f),
            Color.Transparent,
          ), startY = 0f, endY = (winEdge.top * density.density)
        )
      )
  ) {
    if (winState.maximize) {
      WindowTopMaximizedBar(winEdge, winState, win)
    } else {
      WindowTopControllerBar(winEdge, winState, win)
    }
  }
}

@Composable
private fun WindowTopControllerBar(
  winEdge: WindowEdge,
  winState: WindowState,
  win: DesktopWindowController
) {
  val density = LocalDensity.current;
  val coroutineScope = rememberCoroutineScope()
  Row {
    Box(
      modifier = Modifier
        .wrapContentWidth()
        .fillMaxHeight(),
    ) {
      IconButton(
        modifier = Modifier.align(Alignment.CenterStart),
        onClick = { coroutineScope.launch { win.close() } }) {
        Icon(Icons.Rounded.Close, contentDescription = "Close the Window")
      }
    }
    Box(
      modifier = Modifier
        .weight(2f)
        .fillMaxHeight(),
    ) {
      Text(
        modifier = Modifier
          .align(Alignment.Center)
          .padding(2.dp),
        text = win.state.title,
        style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onPrimary)
      )
    }
    Box(
      modifier = Modifier
        .wrapContentWidth()
        .fillMaxHeight(),
    ) {
      IconButton(
        modifier = Modifier.align(Alignment.CenterEnd),
        onClick = { coroutineScope.launch { win.maximize() } }) {
        Icon(
          Icons.Rounded.UnfoldMore,
          contentDescription = "Maximizes the window",
          modifier = Modifier.rotate(45f)
        )
      }
    }
  }
}

/**
 * 最大化模式下，顶部默认不会有东西，因为这里针对移动设备进行设计
 * 这时候的顶部与 save-area-top 一致
 */
@Composable
private fun WindowTopMaximizedBar(
  winEdge: WindowEdge,
  winState: WindowState,
  win: DesktopWindowController
) {
  /// 这里可以渲染一些特殊的信息
}