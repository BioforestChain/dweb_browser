package org.dweb_browser.window.render

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MenuOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.helper.android.AutoResizeTextContainer
import org.dweb_browser.helper.android.AutoSizeText
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.window.core.constant.WindowPropertyKeys
import kotlin.math.min

/**
 * 窗口底部栏
 */
@Composable
internal fun WindowBottomBar(
  win: WindowController, modifier: Modifier = Modifier
) {
  val windowEdge = LocalWindowPadding.current
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  CompositionLocalProvider(
    LocalContentColor provides contentColor,
  ) {
    Box(
      modifier = modifier
        .height(windowEdge.bottom.dp)
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Transparent,
              contentColor.copy(alpha = 0.2f),
            )
          )
        )
    ) {
      val maximize by win.watchedIsMaximized()
      if (maximize) {
        WindowBottomMaximizedBar(win) {
          WindowBottomThemeBar(win)
        }
      } else {
        WindowBottomResizeBar(win) {
          WindowBottomThemeBar(win)
        }
      }
    }
  }
}

/**
 * 可以控制窗口大小的底部控制栏
 */
@Composable
private fun WindowBottomResizeBar(
  win: WindowController,
  content: @Composable () -> Unit,
) {
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  val windowEdge = LocalWindowPadding.current
  val resizable by win.watchedState { resizable }
  Row(
    modifier = Modifier
      .fillMaxSize()
      .padding(
        start = if (resizable) 0.dp else windowEdge.left.dp,
        end = if (resizable) 0.dp else windowEdge.right.dp
      )
  ) {
    /// 左下角 视窗 Resize
    if (resizable) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(windowEdge.bottom.dp)
          .windowResizeByLeftBottom(win)
      ) {
        Icon(
          Icons.Rounded.ChevronLeft, contentDescription = "Resize by Left Bottom Corner",
          modifier = Modifier
            .rotate(-45f)
            .align(Alignment.BottomStart),
          tint = contentColor,
        )
      }
    }
    /// 下方 视窗 Resize
    Box(
      modifier = Modifier
        .fillMaxSize()
        .weight(1f, true)
    ) {
      content()
    }
    /// 右下角
    /// 视窗 Resize
    if (resizable) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(windowEdge.bottom.dp)
          .windowResizeByRightBottom(win)
      ) {
        Icon(
          Icons.Rounded.ChevronRight,
          contentDescription = "Resize by Right Bottom Corner",
          modifier = Modifier
            .rotate(45f)
            .align(Alignment.BottomEnd),
          tint = contentColor,
        )
      }
    }
  }
}

@Composable
private fun WindowBottomMaximizedBar(
  win: WindowController,
  content: @Composable () -> Unit,
) {
  val windowEdge = LocalWindowPadding.current
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(start = windowEdge.start.dp, end = windowEdge.end.dp),
  ) {
    content()
  }
}

/**
 * 风格化的底部栏
 */
@Composable
private fun WindowBottomThemeBar(
  win: WindowController,
) {
  val bottomBarTheme by win.watchedState(watchKey = WindowPropertyKeys.BottomBarTheme) { bottomBarTheme }
  when (bottomBarTheme) {
    WindowBottomBarTheme.Immersion -> WindowBottomImmersionThemeBar(win)
    WindowBottomBarTheme.Navigation -> WindowBottomNavigationThemeBar(win)
  }
}

/**
 * 沉浸模式
 * 可以看到应用的基本信息和取消最大化
 */
@Composable
private fun WindowBottomImmersionThemeBar(
  win: WindowController,
) {
  val winTheme = LocalWindowControllerTheme.current
  val coroutineScope = rememberCoroutineScope()
  Row(modifier = Modifier
    .fillMaxSize()
    .pointerInput(Unit) {
      detectTapGestures(onDoubleTap = {
        coroutineScope.launch {
          win.unMaximize()
        }
      })
    }) {
    /// 应用图标
    Spacer(
      modifier = Modifier
        .weight(0.618f)
        .fillMaxHeight()
    )
    /// 应用标题
    WindowBottomBarInfoText(
      win,
      Modifier.weight(1f),
    )
  }
}

/**
 * 导航模式
 *
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WindowBottomNavigationThemeBar(
  win: WindowController,
) {
  val coroutineScope = rememberCoroutineScope()
  val winTheme = LocalWindowControllerTheme.current
  val contentColor = winTheme.bottomContentColor
  val contentDisableColor = winTheme.bottomContentDisableColor
  val backgroundColor = winTheme.bottomBackgroundColor
  val winPadding = LocalWindowPadding.current
  val bottomBarHeight = winPadding.bottom
  val infoHeight = min(bottomBarHeight * 0.25f, LocalWindowLimits.current.bottomBarBaseHeight)
  val isMaximized by win.watchedIsMaximized()
  val buttonRoundedSize = infoHeight * 2
  Box(modifier = Modifier.fillMaxSize()) {
    /// 按钮
    Row(modifier = Modifier
      .zIndex(1f)
      .pointerInput(Unit) {
        detectTapGestures(onDoubleTap = {
          coroutineScope.launch {
            win.unMaximize()
          }
        })
      }
      .fillMaxSize()
      .padding(bottom = (infoHeight / 2).dp, top = (infoHeight / 3).dp)
      //
    ) {
      val canGoBack by win.watchedState { canGoBack }
      /// 返回按钮
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
      ) {
        when (val enabled = canGoBack) {
          null -> {}
          else -> {
            BackHandler(enabled) {
              coroutineScope.launch {
                win.emitGoBack()
              }
            }
            TextButton(
              onClick = {
                coroutineScope.launch {
                  win.emitGoBack()
                }
              },
              enabled = enabled,
              contentPadding = PaddingValues(0.dp),
              shape = RoundedCornerShape(buttonRoundedSize),
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(),
            ) {
              Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = "Go Back",
                tint = if (enabled) contentColor else contentDisableColor,
                modifier = Modifier.align(Alignment.CenterVertically)
              )
            }
          }
        }
      }
      val canGoForward by win.watchedState { canGoForward }
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
      ) {
        when (val enabled = canGoForward) {
          null -> {}
          else -> {
            TextButton(
              onClick = {
                coroutineScope.launch {
                  win.emitGoForward()
                }
              },
              enabled = enabled,
              contentPadding = PaddingValues(0.dp),
              shape = RoundedCornerShape(buttonRoundedSize),
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(),
            ) {
              Icon(
                Icons.Rounded.ArrowForward,
                contentDescription = "Go Forward",
                tint = if (enabled) contentColor else contentDisableColor,
                modifier = Modifier.align(Alignment.CenterVertically)
              )
            }
          }
        }
      }

//      /// 菜单按钮
//      BoxWithConstraints(
//        modifier = Modifier
//          .weight(1f)
//          .fillMaxHeight()
//      ) {
//        if (isMaximized) {
//          TextButton(
//            onClick = {
//              coroutineScope.launch { win.unMaximize() }
//            },
//            contentPadding = PaddingValues(0.dp),
//            shape = RoundedCornerShape(buttonRoundedSize),
//            modifier = Modifier
//              .align(Alignment.CenterEnd)
//              .fillMaxWidth(),
//          ) {
//            Icon(
//              Icons.Rounded.Menu,
//              contentDescription = "open menu panel",
//              modifier = Modifier.align(Alignment.CenterVertically),
//              tint = contentColor,
//            )
//          }
//        }
//      }
      /// 退出全屏
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
      ) {
        if (isMaximized) {
          TextButton(
            onClick = {
              coroutineScope.launch { win.unMaximize() }
            },
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(buttonRoundedSize),
            modifier = Modifier
              .align(Alignment.CenterEnd)
              .fillMaxWidth(),
          ) {

            Icon(
              Icons.Rounded.FullscreenExit,
              contentDescription = "UnMaximizes the window",
              modifier = Modifier.align(Alignment.CenterVertically),
              tint = contentColor,
            )
          }
        }
      }
    }

    /// 底部文本
    WindowBottomBarInfoText(
      win,
      Modifier
        .height(infoHeight.dp)
        .fillMaxWidth(0.618f)
        .zIndex(2f)
        .align(Alignment.BottomCenter)
        .pointerInteropFilter { false },
    )
  }

}

/**
 * 应用标题
 * 会根据容器进行自适应排布
 */
@Composable
fun WindowBottomBarInfoText(win: WindowController, modifier: Modifier) {
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  AutoResizeTextContainer(modifier.fillMaxHeight()) {
    val footerText by win.watchedState { owner }
    val textStyle = MaterialTheme.typography.bodySmall
    AutoSizeText(text = footerText,
      color = contentColor,
      style = textStyle,
      modifier = Modifier.align(Alignment.Center),
      overflow = TextOverflow.Visible,
      softWrap = false,
      autoLineHeight = { it * 1.25f })
  }
}
