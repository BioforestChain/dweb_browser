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
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.core.WindowController
import kotlinx.coroutines.launch

@Composable
internal fun WindowTopBar(
  win: WindowController,
) {
  val winPadding = LocalWindowPadding.current;
  val contentColor = LocalWindowControllerTheme.current.topContentColor
  Box(
    modifier =
    Modifier
      .windowMoveAble(win)
      .fillMaxWidth()
      .height(winPadding.top.dp)
      .background(
        Brush.verticalGradient(
          colors = listOf(
            contentColor.copy(alpha = 0.2f),
            Color.Transparent,
          )
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
  win: WindowController,
) {
  val coroutineScope = rememberCoroutineScope()
  val contentColor = LocalWindowControllerTheme.current.topContentColor
  Row {
    /// 关闭按钮
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
    /// 标题信息
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
    /// 最大化
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
  win: WindowController,
) {
  /// 这里可以渲染一些特殊的信息
}