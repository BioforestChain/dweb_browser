package info.bagen.dwebbrowser.microService.browser.desk.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.UnfoldLess
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.desk.DesktopWindowController
import info.bagen.dwebbrowser.microService.core.WindowState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 窗口底部栏
 */
@Composable
internal fun WindowBottomBar(
  win: DesktopWindowController,
  windowEdge: WindowEdge,
  winState: WindowState,
  emitWinStateChange: () -> Job,
) {
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
    if (winState.maximize) {
      WindowBottomMaximizedBar(
        win,
        windowEdge,
        winState,
        emitWinStateChange,
      )
    } else {
      WindowBottomResizeBar(
        win,
        windowEdge,
        winState,
        emitWinStateChange,
      )
    }
  }
}

/**
 * 可以控制窗口大小的底部控制栏
 */
@Composable
private fun WindowBottomResizeBar(
  win: DesktopWindowController,
  windowEdge: WindowEdge,
  winState: WindowState,
  emitWinStateChange: () -> Job,
) {
  val density = LocalDensity.current
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    /// 左下角 视窗 Resize
    Box(modifier = Modifier
      .weight(1f)
      .fillMaxHeight()
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          winState.bounds.left += dragAmount.x / density.density
          winState.bounds.width -= dragAmount.x / density.density
          winState.bounds.height += dragAmount.y / density.density
          emitWinStateChange()
        }
      }) {
      Icon(
        Icons.Rounded.ChevronLeft, contentDescription = "Resize by Left Bottom Corner",
        modifier = Modifier
          .rotate(-45f)
          .align(Alignment.CenterStart)
      )
    }
    /// 下方 视窗 Resize
    Box(modifier = Modifier
      .weight(2f)
      .fillMaxHeight()
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          winState.bounds.height += dragAmount.y / density.density
          emitWinStateChange()
        }
      }) {
      Icon(
        Icons.Rounded.DragHandle, contentDescription = "Resize Height",
        modifier = Modifier
          .align(Alignment.Center)
      )
    }
    /// 右下角
    /// 视窗 Resize
    Box(modifier = Modifier
      .weight(1f)
      .fillMaxHeight()
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          winState.bounds.width += dragAmount.x / density.density
          winState.bounds.height += dragAmount.y / density.density
          emitWinStateChange()
        }
      }) {
      Icon(
        Icons.Rounded.ChevronRight,
        contentDescription = "Resize by Right Bottom Corner",
        modifier = Modifier
          .rotate(45f)
          .align(Alignment.CenterEnd)
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
  windowEdge: WindowEdge,
  winState: WindowState,
  emitWinStateChange: () -> Job,
) {
  val density = LocalDensity.current
  val coroutineScope = rememberCoroutineScope()
  Row(
    modifier = Modifier.fillMaxSize()
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
      Text(
        text = winState.title, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
          .align(Alignment.Center)
      )
    }
  }
}