package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.desk.DesktopWindowController
import kotlinx.coroutines.launch

@Composable
internal fun WindowTopBar(
  win: DesktopWindowController,
) {
  val density = LocalDensity.current;
  val winEdge = LocalWindowEdge.current;
  Box(
    modifier =
    Modifier
      .windowMoveAble(win)
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
    val maximize by win.watchedIsMaximized()
    if (maximize) {
      WindowTopMaximizedBar(win)
    } else {
      WindowTopControllerBar(win)
    }
  }
}

@Composable
private fun WindowTopControllerBar(
  win: DesktopWindowController,
) {
  val coroutineScope = rememberCoroutineScope()
  val contentColor = LocalWindowControllerTheme.current.topContentColor
  Row {
    Box(
      modifier = Modifier
        .wrapContentWidth()
        .fillMaxHeight(),
    ) {
      IconButton(modifier = Modifier.align(Alignment.CenterStart),
        onClick = { coroutineScope.launch { win.close() } }) {
        Icon(Icons.Rounded.Close, contentDescription = "Close the Window", tint = contentColor)
      }
    }
    Box(
      modifier = Modifier
        .weight(2f)
        .fillMaxHeight(),
    ) {
      val title_text by win.watchedState { title ?: owner }
      Text(
        modifier = Modifier
          .align(Alignment.Center)
          .padding(2.dp),
        text = title_text,
        style = MaterialTheme.typography.titleSmall.copy(color = contentColor)
      )
    }
    Box(
      modifier = Modifier
        .wrapContentWidth()
        .fillMaxHeight(),
    ) {
      IconButton(modifier = Modifier.align(Alignment.CenterEnd),
        onClick = { coroutineScope.launch { win.maximize() } }) {
        Icon(
          Icons.Rounded.UnfoldMore,
          contentDescription = "Maximizes the window",
          modifier = Modifier.rotate(45f),
          tint = contentColor
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
  win: DesktopWindowController,
) {
  /// 这里可以渲染一些特殊的信息
}