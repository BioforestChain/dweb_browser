package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.WindowPlacement
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Rect
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.platform.ComposeWindowParams
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import java.awt.Point
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.event.WindowEvent
import java.awt.event.WindowListener

@Composable
fun WindowController.WindowControllerEffect() {
  val viewController = LocalPureViewController.current.asDesktop()
  val renderConfig = state.renderConfig

  val composeWindow by viewController.composeWindowAsState()
  val composeWindowParams = viewController.composeWindowParams


  /**
   * 坐标的双向绑定
   * 为了实时性，这里不使用 composeWindowParams
   */
  LaunchedEffect(composeWindow, renderConfig, viewController) {
    /**
     * 拖动窗口的起点 输入位置（相对窗口的位置）
     */
    var dragStartPoint: Point? = null
    renderConfig.useCustomFrameDrag = WindowState.WindowRenderConfig.FrameDragListener(
      frameDragStart = {
        /**
         * composeWindow.mousePosition 是指鼠标相对于窗口左上角的位置，会有负数
         */
        dragStartPoint = composeWindow.mousePosition
      },
      frameDragMove = {},
      frameDragEnd = { dragStartPoint = null },
    )
//    renderConfig.frameDragStart = { x, y ->
//      dragStartRelativeX = (state.bounds.x - x).toInt()
//      dragStartRelativeY = (state.bounds.y - y).toInt()
//    }

    /// TODO 设置窗口的最小值，避免resize过小导致布局计算异常
//    composeWindow.minimumSize

    composeWindow.addWindowStateListener {
//      println("QAQ addWindowStateListener")
    }
    composeWindow.addComponentListener(object : ComponentListener {
      /**
       *  虽然compose自己有自动resize的行为，但是这里需要去updateBounds，并提供 Outer 的 reason。
       *  这样这个在重新绘制阶段，calcWindowBoundsByLimits 就不会更新 bounds，就不会导致 inner=>outer 的同步
       */
      override fun componentResized(event: ComponentEvent) {
        state.updateBounds(WindowState.UpdateBoundsReason.Outer) {
          copy(width = composeWindow.width.toFloat(), height = composeWindow.height.toFloat())
        }
      }

      override fun componentMoved(event: ComponentEvent) {
        /// 如果正在拖动中，那么这里不进行同步
        if (dragStartPoint == null) {
          /// 这里会在 left、top 在 resize 的时候同时触发 move
          state.updateBounds(WindowState.UpdateBoundsReason.Outer) {
            copy(x = composeWindow.x.toFloat(), y = composeWindow.y.toFloat())
          }
        }
      }

      override fun componentShown(event: ComponentEvent) {
//        println("QAQ componentShown")
      }

      override fun componentHidden(event: ComponentEvent) {
//        println("QAQ componentHidden")
      }
    })
    composeWindow.addPropertyChangeListener {
//      println("QAQ addPropertyChangeListener ${it.propertyName}=${it.oldValue}=>${it.oldValue}")
    }
    composeWindow.addHierarchyListener {
//      println("QAQ addHierarchyListener")
    }
    composeWindow.addMouseMotionListener(object : MouseMotionListener {
      override fun mouseDragged(event: MouseEvent) {
        val startPoint = dragStartPoint
        if (startPoint != null) {
          val windowX = event.xOnScreen - startPoint.x
          val windowY = event.yOnScreen - startPoint.y
          // 更新内部状态
          state.updateBounds(WindowState.UpdateBoundsReason.Outer) {
            copy(x = windowX.toFloat(), y = windowY.toFloat())
          }
          // 更新原生窗口
          composeWindow.setLocation(windowX, windowY)
        }
      }

      override fun mouseMoved(p0: MouseEvent?) {
      }
    })
    state.observable.onChange {
      if (it.key == WindowPropertyKeys.Bounds) {
        if (state.updateBoundsReason == WindowState.UpdateBoundsReason.Inner) {
          val newBounds = it.newValue as Rect
          composeWindow.setBounds(
            newBounds.x.toInt(),
            newBounds.y.toInt(),
            newBounds.width.toInt(),
            newBounds.height.toInt(),
          )
        }
      }
    }
  }

  TitleEffect(composeWindow)
  VisibleEffect(composeWindowParams)
  ModeEffect(composeWindow, composeWindowParams)
}

/**
 * title 的单向绑定
 */
@Composable
private fun WindowController.TitleEffect(composeWindow: ComposeWindow) {
  val title by watchedState { title }
  LaunchedEffect(composeWindow, title) {
    composeWindow.title = title
  }
}

/**
 * visible 双向绑定
 */
@Composable
private fun WindowController.VisibleEffect(
  composeWindowParams: ComposeWindowParams,
) {
  /// 因为原生的窗口隐藏会导致compose停止渲染，所以这里不能用 LaunchedEffect 的 remember 特性来做绑定
  SideEffect {
    state.observable.onChange {
      if (it.key == WindowPropertyKeys.Visible) {
        composeWindowParams.visible = it.newValue as Boolean
      }
    }
  }
  watchedIsMaximized()
  /// 反向绑定原生的窗口状态到state中
  LaunchedEffect(composeWindowParams.visible) {
    println("QAQ composeWindowParams.visible=>state.visible | ${composeWindowParams.visible}")
    state.visible = composeWindowParams.visible
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
  /// 因为原生的窗口隐藏会导致compose停止渲染，所以这里不能用 LaunchedEffect 的 remember 特性来做绑定
  SideEffect {
    state.observable.onChange {
      if (it.key == WindowPropertyKeys.Mode) {
        when (it.newValue as WindowMode) {
          WindowMode.FLOAT -> composeWindowParams.placement = WindowPlacement.Floating
          WindowMode.MAXIMIZE -> composeWindowParams.placement = WindowPlacement.Maximized
          WindowMode.FULLSCREEN -> composeWindowParams.placement = WindowPlacement.Fullscreen
          WindowMode.PIP -> WARNING("ComposeWindow No Support PIP")
          WindowMode.CLOSE -> composeWindowParams.closeWindow()
        }
      }
    }
  }
  watchedIsMaximized()

  /// 反向绑定原生的窗口的 placement 到state中
  LaunchedEffect(composeWindowParams.placement) {
    state.mode = when (composeWindowParams.placement) {
      WindowPlacement.Floating -> WindowMode.FLOAT
      WindowPlacement.Maximized -> WindowMode.MAXIMIZE
      WindowPlacement.Fullscreen -> WindowMode.FULLSCREEN
    }
  }

  /// 反向绑定原生的窗口的 close 到state中
  SideEffect {
    composeWindow.addWindowListener(object : WindowListener {
      override fun windowOpened(p0: WindowEvent) {
      }

      override fun windowClosing(p0: WindowEvent) {
      }

      override fun windowClosed(p0: WindowEvent) {
        lifecycleScope.launch {
          closeRoot(true)
        }
      }

      override fun windowIconified(p0: WindowEvent) {
      }

      override fun windowDeiconified(p0: WindowEvent) {
      }

      override fun windowActivated(p0: WindowEvent) {
      }

      override fun windowDeactivated(p0: WindowEvent) {
      }
    })
  }
}
