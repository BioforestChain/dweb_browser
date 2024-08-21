package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.platform.ComposeWindowParams
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import org.dweb_browser.sys.window.core.renderConfig.FrameDragDelegate
import org.dweb_browser.sys.window.helper.watchedIsMaximized
import org.dweb_browser.sys.window.helper.watchedState
import java.awt.Point

@Composable
fun WindowController.WindowControllerEffect() {
  val viewController = LocalPureViewController.current.asDesktop()
  val renderConfig = state.renderConfig

  val composeWindow by viewController.composeWindowAsState()
  val composeWindowParams = viewController.composeWindowParams

  val density = LocalDensity.current.density
  /**
   * 坐标的双向绑定
   * 为了实时性，这里不使用 composeWindowParams
   *
   * TODO 设置窗口 composeWindow.minimumSize 的最小值，避免resize过小导致布局计算异常
   */
  LaunchedEffect(composeWindow, renderConfig, viewController, density) {
    /**
     * 拖动窗口的起点 输入位置（相对窗口的位置）
     */
    var dragStartPoint: Point? = null
    renderConfig.frameMoveDelegate = FrameDragDelegate(
      onStart = {
        /**
         * composeWindow.mousePosition 是指鼠标相对于窗口左上角的位置，会有负数
         */
        /**
         * composeWindow.mousePosition 是指鼠标相对于窗口左上角的位置，会有负数
         */
        dragStartPoint = composeWindow.mousePosition
      },
      onEnd = { dragStartPoint = null },
    )

    // 监听原生窗口resize的变化，去改变state的bounds
    launch {
      composeWindowParams.componentEvents.componentResized.collect {
        // 更新到外部，表示是从原生窗口同步而来到状态
        state.updateBounds(WindowState.UpdateReason.Outer) {
          copy(width = composeWindow.width / density, height = composeWindow.height / density)
        }
      }
    }
    launch {
      composeWindowParams.componentEvents.componentMoved.collect {
        /// 如果正在拖动中，那么这里不进行同步
        if (dragStartPoint == null) {
          /// 这里会在 left、top 在 resize 的时候同时触发 move
          state.updateBounds(WindowState.UpdateReason.Outer) {
            copy(x = composeWindow.x / density, y = composeWindow.y / density)
          }
        }
      }
    }
    launch {
      composeWindowParams.mouseMotionEvents.mouseDragged.collect { event ->
        val startPoint = dragStartPoint
        if (startPoint != null) {
          val windowX = event.xOnScreen - startPoint.x
          val windowY = event.yOnScreen - startPoint.y
          // 更新内部状态
          state.updateBounds(WindowState.UpdateReason.Outer) {
            copy(x = windowX / density, y = windowY / density)
          }
          // 更新原生窗口
          composeWindow.setLocation(windowX, windowY)
        }
      }
    }
    // 监听 state的bounds 变化
    state.observable.onChange {
      if (it.key == WindowPropertyKeys.Bounds) {
        // 如果需要更新到内部
        if (state.updateBoundsReason == WindowState.UpdateReason.Inner) {
          val newBoundsPx = (it.newValue as PureRect).timesToInt(density)
          // TODO waterbang 这里设置有bug待验证，设置过快会导致addComponentListener.componentResized 莫名的自己回滚，并且不是记忆窗口的问题
          delay(100)
          composeWindow.setBounds(
            newBoundsPx.x,
            newBoundsPx.y,
            newBoundsPx.width,
            newBoundsPx.height,
          )
        }
      }
    }
  }
  // 把状态都作用到窗口上
  TitleEffect(composeWindowParams)
  IconEffect(composeWindowParams)
  VisibleEffect(composeWindowParams)
  ModeEffect(composeWindow, composeWindowParams)
  FocusEffect(composeWindow, composeWindowParams)
}

/**
 * title 的单向绑定
 */
@Composable
private fun WindowController.TitleEffect(composeWindowParams: ComposeWindowParams) {
  val title by watchedState { title }
  LaunchedEffect(composeWindowParams, title) {
    composeWindowParams.title = title ?: ""
  }
}


/**
 * icon 的单向绑定
 */
@Composable
private fun WindowController.IconEffect(composeWindowParams: ComposeWindowParams) {
  val iconUrl by watchedState { iconUrl }
  composeWindowParams.icon = iconUrl?.let { url ->
    PureImageLoader.SmartLoad(
      url = url,
      maxWidth = 64.dp,
      maxHeight = 64.dp,
      currentColor = null,
      hook = state.constants.microModule.value?.blobFetchHook
    ).success?.toAwtImage()?.toPainter()
  }
}


/**
 * 因为原生的窗口隐藏会导致compose停止渲染，如果用 LaunchedEffect，它允许异步，在窗口隐藏的时候，这个异步会跟随着暂停渲染。
 * 所以这里提供了 RememberEffect，禁止了 LaunchedEffect 的异步特性，只是为了用它的 remember 特性来做绑定
 */
@Composable
private fun RememberEffect(key1: Any?, block: () -> Unit) {
  LaunchedEffect(key1 = key1) {
    block()
  }
}

/**
 * visible 双向绑定
 */
@Composable
private fun WindowController.VisibleEffect(
  composeWindowParams: ComposeWindowParams,
) {
  RememberEffect(composeWindowParams) {
    state.observable.onChange {
      if (it.key == WindowPropertyKeys.Visible) {
        composeWindowParams.isMinimized = !(it.newValue as Boolean)
      }
    }
  }
  watchedIsMaximized()
  /// 反向绑定原生的窗口状态到state中
  LaunchedEffect(composeWindowParams.isMinimized) {
    state.visible = !composeWindowParams.isMinimized
  }
}

/**
 * mode 双向绑定
 * 包括窗口关闭的双向绑定
 */
@Composable
private fun WindowController.ModeEffect(
  composeWindow: ComposeWindow,
  composeWindowParams: ComposeWindowParams,
) {
  RememberEffect(composeWindowParams) {
    state.observable.onChange {
      // 针对固定窗口大小的绑定
      when (it.key) {
        WindowPropertyKeys.Resizable -> {
          composeWindow.isResizable = it.newValue as Boolean
        }
        // 针对桌面端原生窗口的浮动 最大化 全屏 绑定
        WindowPropertyKeys.Mode -> {
          when (it.newValue as WindowMode) {
            WindowMode.FLOAT -> {
              composeWindow.placement = WindowPlacement.Floating
            }

            WindowMode.MAXIMIZE -> {
              composeWindow.placement = WindowPlacement.Maximized
            }

            WindowMode.FULLSCREEN -> {
              composeWindow.placement = WindowPlacement.Fullscreen
            }

            WindowMode.PIP -> WARNING("ComposeWindow No Support PIP")
          }
        }

        WindowPropertyKeys.Closed -> {
          if (it.newValue as Boolean) {
            composeWindowParams.closeWindow()
          }
        }

        else -> {}
      }
    }
  }
  /// 反向绑定原生的窗口的 placement 到state中
  LaunchedEffect(composeWindowParams.placement) {
    state.mode = when (composeWindowParams.placement) {
      WindowPlacement.Floating -> WindowMode.FLOAT
      WindowPlacement.Maximized -> WindowMode.MAXIMIZE
      WindowPlacement.Fullscreen -> WindowMode.FULLSCREEN
    }
  }

  /// 反向绑定原生的窗口的 close 到 state中
  LaunchedEffect(composeWindowParams) {
    lifecycleScope.launch {
      composeWindowParams.windowEvents.windowClosed.collect {
        closeRoot(true)
      }
    }
  }
}

/**
 * focus 单向绑定
 */
@Composable
private fun WindowController.FocusEffect(
  composeWindow: ComposeWindow,
  composeWindowParams: ComposeWindowParams,
) {
  LaunchedEffect(composeWindow) {
    state.observable.onChange {
      if (it.key == WindowPropertyKeys.Focus) {
        when (it.newValue as Boolean) {
          true -> if (!composeWindow.isFocused) {
            composeWindow.toFront()
          }

          false -> if (composeWindow.isFocused) {
            WARNING("ComposeWindow No Support Blur")
          }
        }
      }
    }
  }

  LaunchedEffect(composeWindowParams) {
    composeWindowParams.windowFocusStateFlow.collect {
      when (it) {
        ComposeWindowParams.AwtWindowFocusState.Gained -> focus()
        ComposeWindowParams.AwtWindowFocusState.Lost -> blur()
      }
    }
  }
}