package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.desk.DesktopWindowController
import kotlinx.coroutines.launch

/**
 * 窗口底部栏
 */
@Composable
internal fun WindowBottomBar(
  win: DesktopWindowController,
) {
  val windowEdge = LocalWindowPadding.current
  val density = LocalDensity.current
  Box(
    modifier = Modifier
      .height(windowEdge.bottom.dp)
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.2f),
          ), startY = 0f, endY = (windowEdge.bottom * density.density)
        )
      )
  ) {
    val maximize by win.watchedIsMaximized()
    if (maximize) {
      WindowBottomMaximizedBar(win)
    } else {
      WindowBottomResizeBar(win)
    }
  }
}

/**
 * 可以控制窗口大小的底部控制栏
 */
@Composable
private fun WindowBottomResizeBar(
  win: DesktopWindowController,
) {
  val density = LocalDensity.current
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    /// 左下角 视窗 Resize
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .windowResizeByLeftBottom(win)
    ) {
      Icon(
        Icons.Rounded.ChevronLeft, contentDescription = "Resize by Left Bottom Corner",
        modifier = Modifier
          .rotate(-45f)
          .align(Alignment.CenterStart),
        tint = contentColor,
      )
    }
    /// 下方 视窗 Resize
    Box(modifier = Modifier
      .weight(2f)
      .fillMaxHeight()
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          win.state.updateMutableBounds {
            height += dragAmount.y / density.density
          }
        }
      }) {
      Icon(
        Icons.Rounded.DragHandle, contentDescription = "Resize Height",
        modifier = Modifier
          .align(Alignment.Center),
        tint = contentColor,
      )
    }
    /// 右下角
    /// 视窗 Resize
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .windowResizeByRightBottom(win)
    ) {
      Icon(
        Icons.Rounded.ChevronRight,
        contentDescription = "Resize by Right Bottom Corner",
        modifier = Modifier
          .rotate(45f)
          .align(Alignment.CenterEnd),
        tint = contentColor,
      )
    }
  }
}

/**
 * 窗口在最大化是的底部控制条
 * 可以看到应用的基本信息和取消最大化
 */
@Composable
private fun WindowBottomMaximizedBar(
  win: DesktopWindowController,
) {
  val winTheme = LocalWindowControllerTheme.current
  val coroutineScope = rememberCoroutineScope()
  Row(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(Unit) {
        detectTapGestures(onDoubleTap = {
          coroutineScope.launch {
            win.unMaximize()
          }
        })
      }
  ) {
    /// 应用图标
    Box(
      modifier = Modifier
        .weight(0.618f)
        .fillMaxHeight()
    ) {
//      var iconState by rememberAsyncImagePainter(model = )
//      AsyncImage(
//        model = winState.iconUrl,
//        contentScale = ContentScale.Inside,
//        contentDescription = "Window Icon",
//       onState =
//      )
    }
    /// 应用标题
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    ) {
      val footer_text by win.watchedState { owner }
      Text(
        text = footer_text,
        style = MaterialTheme.typography.labelSmall.copy(color = winTheme.bottomContentColor),
        modifier = Modifier
          .align(Alignment.Center)
      )
    }
  }
}