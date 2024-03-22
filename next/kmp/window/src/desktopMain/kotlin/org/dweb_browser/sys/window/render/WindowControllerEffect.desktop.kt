package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import org.dweb_browser.helper.Rect
import org.dweb_browser.helper.platform.ComposeWindowParams
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import java.awt.Point
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener

@Composable
fun WindowController.WindowControllerEffect() {
  val viewController = LocalPureViewController.current.asDesktop()
//  val composeWindowParams = remember(viewController) { viewController.composeWindowParams }
  val renderConfig = state.renderConfig
  val scope = rememberCoroutineScope()
  LaunchedEffect(renderConfig, viewController) {
    /**
     * composeWindow.mousePosition 是指鼠标相对于窗口左上角的位置，会有负数
     */
    val composeWindow = viewController.awaitComposeWindow()

    /**
     * 拖动窗口的起点 输入位置（相对窗口的位置）
     */
    var dragStartPoint: Point? = null
    renderConfig.useCustomFrameDrag = WindowState.WindowRenderConfig.FrameDragListener(
      frameDragStart = {
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
//
//  /// 绑定 bounds 到 原生窗口
//  DisposableEffect(composeWindowParams) {
//    composeWindowParams.bindBounds(state.bounds);
//    val off = state.observable.onChange {
//      if (it.key == WindowPropertyKeys.Bounds) {
//        composeWindowParams.bindBounds(it.newValue as Rect);
//      }
//    }
//    onDispose {
//      off()
//    }
//  }
//  val title by watchedState { title }
//  LaunchedEffect(composeWindowParams, title) {
//    composeWindowParams.title = title ?: ""
//  }
}

private fun ComposeWindowParams.bindBounds(winBounds: Rect) {
  position = WindowPosition.Absolute(winBounds.x.dp, winBounds.y.dp)
  size = DpSize(winBounds.width.dp, winBounds.height.dp)
}