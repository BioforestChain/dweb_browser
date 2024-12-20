package org.dweb_browser.sys.window.render

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.NativeBackHandler
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.LocalWindowFrameStyle
import org.dweb_browser.sys.window.helper.LocalWindowLimits
import org.dweb_browser.sys.window.helper.watchedIsMaximized
import org.dweb_browser.sys.window.helper.watchedState
import kotlin.math.max
import kotlin.math.min

/**
 * 窗口底部栏
 */
@Composable
internal fun WindowBottomBar(
  win: WindowController, modifier: Modifier = Modifier,
) {
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  CompositionLocalProvider(
    LocalContentColor provides contentColor,
  ) {
    Box(
      modifier = modifier.background(
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
internal fun WindowBottomResizeBar(
  win: WindowController,
  content: @Composable () -> Unit,
) {
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  val windowEdge = LocalWindowFrameStyle.current
  val resizable by win.watchedState { resizable }
  Row(
    modifier = Modifier.fillMaxSize().padding(
      start = if (resizable) 0.dp else windowEdge.startWidth.dp,
      end = if (resizable) 0.dp else windowEdge.endWidth.dp
    )
  ) {
    // 如果使用 原生窗口，那么不显示这两个角标
    val showResizeIcon = resizable && win.state.renderConfig.isWindowUseComposeFrame
    /// 左下角 视窗 Resize
    if (showResizeIcon) {
      Box(
        modifier = Modifier.fillMaxHeight().width(windowEdge.frameSize.bottom.dp)
          .windowResizeByLeftBottom(win)
      ) {
        Icon(
          Icons.Rounded.ChevronLeft, contentDescription = "Resize by Left Bottom Corner",
          modifier = Modifier.rotate(-45f).align(Alignment.BottomStart),
          tint = contentColor,
        )
      }
    }
    /// 下方 视窗 Resize
    Box(
      modifier = Modifier.fillMaxSize().weight(1f, true)
    ) {
      content()
    }
    /// 右下角
    /// 视窗 Resize
    if (showResizeIcon) {
      Box(
        modifier = Modifier.fillMaxHeight().width(windowEdge.frameSize.bottom.dp)
          .windowResizeByRightBottom(win)
      ) {
        Icon(
          Icons.Rounded.ChevronRight,
          contentDescription = "Resize by Right Bottom Corner",
          modifier = Modifier.rotate(45f).align(Alignment.BottomEnd),
          tint = contentColor,
        )
      }
    }
  }
}

@Composable
internal fun WindowBottomMaximizedBar(
  @Suppress("UNUSED_PARAMETER") win: WindowController,
  content: @Composable () -> Unit,
) {
  val windowEdge = LocalWindowFrameStyle.current
  Box(
    modifier = Modifier.fillMaxSize()
      .padding(start = windowEdge.startWidth.dp, end = windowEdge.endWidth.dp),
  ) {
    content()
  }
}

/**
 * 风格化的底部栏
 */
@Composable
internal fun WindowBottomThemeBar(
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
internal fun WindowBottomImmersionThemeBar(
  win: WindowController,
  modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  Row(modifier = modifier.fillMaxSize().pointerInput(Unit) {
    detectTapGestures(onDoubleTap = {
      coroutineScope.launch {
        win.unMaximize()
      }
    })
  }) {
    /// 应用标题
    WindowBottomBarInfoText(
      win,
      Modifier.fillMaxSize(),
    )
  }
}

@Composable
expect fun windowBottomNavigationThemeBarMaximizedModifier(): Modifier

/**
 * 导航模式
 *
 */
@Composable
internal fun WindowBottomNavigationThemeBar(
  win: WindowController,
) {
  val scope = rememberCoroutineScope()
  val winTheme = LocalWindowControllerTheme.current
  val contentColor = winTheme.bottomContentColor
  val contentDisableColor = winTheme.bottomContentDisableColor
  val winFrameStyle = LocalWindowFrameStyle.current
  val bottomBarHeight = winFrameStyle.frameSize.bottom
  val boxSafeAreaInsets = winFrameStyle.frameSafeAreaInsets
  val infoHeight = min(bottomBarHeight * 0.25f, LocalWindowLimits.current.bottomBarBaseHeight)
  val isMaximized by win.watchedIsMaximized()
  val buttonRoundedSize = infoHeight * 2
  val infoTextHeight =
    if (boxSafeAreaInsets.bottom > 0 && isMaximized) boxSafeAreaInsets.bottom else infoHeight
  Box(modifier = Modifier.fillMaxSize()) {
    var paddingTop = infoHeight / 3
    var paddingBottom = infoHeight / 2
    if (boxSafeAreaInsets.bottom != 0f) {
      val totalPadding = paddingTop + paddingBottom
      paddingTop = max(0f, totalPadding - boxSafeAreaInsets.bottom)
      paddingBottom = totalPadding - paddingTop
    }
    val buttonsModifier = when {
      isMaximized -> windowBottomNavigationThemeBarMaximizedModifier()
      else -> Modifier
    }
    /// 按钮
    Row(
      modifier = buttonsModifier.zIndex(1f).pointerInput(Unit) {
        detectTapGestures(onDoubleTap = {
          scope.launch {
            win.unMaximize()
          }
        })
      }.fillMaxSize().padding(top = paddingTop.dp, bottom = paddingBottom.dp)
      //
    ) {
      val canGoBack by win.watchedState(watchKey = WindowPropertyKeys.CanGoBack) { canGoBack }
      /// 后退或关闭按钮
      Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
        /// 在最大化的情况下，如果不能后退，那么显示关闭
        val isShowCloseBtn = isMaximized && canGoBack != true

        if (isShowCloseBtn) {
          /// 关闭按钮
          TextButton(
            onClick = { scope.launch { win.tryCloseOrHide() } },
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(buttonRoundedSize),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
          ) {
            Icon(
              Icons.Rounded.Close,
              contentDescription = "Close the Window",
              tint = contentColor,
              modifier = Modifier.align(Alignment.CenterVertically)
            )
          }
        } else when (val btnCanGoBack = canGoBack) {
          null -> {}
          else -> {
            // 窗口是否聚焦，如果聚焦，那么将会拦截系统的返回事件
            val isFocus by win.watchedState { focus }

            /**
             * 尝试返回或者关闭窗口
             */
            fun goBackOrClose(canBack: Boolean) {
              scope.launch {
                // 不能直接将整个应用切换到后台，而是关闭当前应用
                if (canBack) {
                  win.navigation.emitGoBack()
                } else {
                  win.tryCloseOrHide()
                }
              }
            }
            /// 监听物理返回按钮，只有当前聚焦的窗口可以执行这个监听
            NativeBackHandler(isFocus) {
              goBackOrClose(btnCanGoBack)
            }
            /// 返回按钮
            TextButton(
              onClick = {
                goBackOrClose(btnCanGoBack)
              },
              enabled = btnCanGoBack,
              contentPadding = PaddingValues(0.dp),
              shape = RoundedCornerShape(buttonRoundedSize),
              modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Go Back",
                tint = if (btnCanGoBack) contentColor else contentDisableColor,
                modifier = Modifier.align(Alignment.CenterVertically)
              )
            }
          }
        }
      }
      val canGoForward by win.watchedState { canGoForward }
      /// 前进按钮
      BoxWithConstraints(
        modifier = Modifier.weight(1f).fillMaxHeight()
      ) {
        when (val enabled = canGoForward) {
          null -> {}
          else -> {
            TextButton(
              onClick = {
                scope.launch {
                  win.navigation.emitGoForward()
                }
              },
              enabled = enabled,
              contentPadding = PaddingValues(0.dp),
              shape = RoundedCornerShape(buttonRoundedSize),
              modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Go Forward",
                tint = if (enabled) contentColor else contentDisableColor,
                modifier = Modifier.align(Alignment.CenterVertically)
              )
            }
          }
        }
      }

      WindowBottomBarMenuPanel(win)
      // 在resizable 的时候禁止退出全屏
      val resizable by win.watchedState { resizable }
      /// 退出全屏
      if (isMaximized && resizable) {
        BoxWithConstraints(
          modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
          TextButton(
            onClick = {
              scope.launch { win.unMaximize() }
            },
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(buttonRoundedSize),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
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
      Modifier.height(infoTextHeight.dp).fillMaxWidth(0.618f).zIndex(2f)
        .align(Alignment.BottomCenter)
//        .pointerInteropFilter { false },
    )
  }
}

// desktop的Menu使用图标，因此需要分开实现
@Composable
expect fun RowScope.WindowBottomBarMenuPanel(win: WindowController)

/**
 * 应用标题
 * 会根据容器进行自适应排布
 */
@Composable
fun WindowBottomBarInfoText(win: WindowController, modifier: Modifier) {
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  win.IdRender(modifier, contentColor)
}

