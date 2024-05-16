package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.asDesktop
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.platform.getComposeWindowOrNull
import java.awt.Color
import java.awt.Rectangle
import java.awt.Shape
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.geom.RoundRectangle2D
import javax.swing.JDialog

actual suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView =
  TaskbarView.from(taskbarController)

class TaskbarView private constructor(
  private val taskbarController: TaskbarController, override val taskbarDWebView: IDWebView,
) : ITaskbarView(taskbarController) {
  companion object {
    suspend fun from(taskbarController: TaskbarController) = TaskbarView(
      taskbarController, IDWebView.create(
        taskbarController.deskNMM,
        taskbarController.getTaskbarDWebViewOptions(),
      )
    )
  }

  class TaskbarDialog(val taskbarController: TaskbarController, val taskbarDWebView: DWebView) :
    JDialog() {
    private lateinit var contentShape: Shape
    val round = 20
    val padding = 5

    init {
      effectStyle(this)
    }

    companion object {
      private fun effectStyle(dialog: TaskbarDialog) {
        dialog.isUndecorated = true; // 移除窗体装饰
        dialog.background = dialog.background.run { Color(red, green, blue, alpha / 2) }

        // 设置窗口圆角
        val round = dialog.round.toDouble()
        dialog.contentShape = RoundRectangle2D.Double(
          0.0, 0.0, dialog.width.toDouble(), dialog.height.toDouble(), round, round
        )
        dialog.shape = dialog.contentShape

        // 禁止焦点
        dialog.focusableWindowState = false
        dialog.isModal = false
        // 置顶
        dialog.isAlwaysOnTop = true

        val rootView = dialog.taskbarDWebView.viewEngine.wrapperView
        dialog.add(rootView)

        // 位置
        dialog.taskbarController.deskNMM.getComposeWindowOrNull()?.also { composeWindow ->
          fun autoTaskbarPosition(parentBounds: Rectangle = composeWindow.bounds) {
            val selfBounds = dialog.bounds
            val x = parentBounds.x + parentBounds.width + dialog.padding
            val y = parentBounds.y + parentBounds.height / 2 - selfBounds.height / 2
            if (x != selfBounds.x || y != selfBounds.y) {
              dialog.setLocation(x, y)
            }
          }
          dialog.addComponentListener(object : ComponentListener {
            override fun componentResized(p0: ComponentEvent?) {
              autoTaskbarPosition()
            }

            override fun componentMoved(p0: ComponentEvent?) {
            }

            override fun componentShown(p0: ComponentEvent?) {
            }

            override fun componentHidden(p0: ComponentEvent?) {
            }
          })
          composeWindow.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(p0: ComponentEvent?) {
              autoTaskbarPosition()
            }

            override fun componentMoved(p0: ComponentEvent?) {
              autoTaskbarPosition()
            }

          })

          // 初始化位置
          autoTaskbarPosition()


//          /// 初始化位置
//          fun autoDesktopPosition() {
//
//            /// 获取安全区域
//            with(composeWindow.toolkit.getScreenInsets(composeWindow.graphicsConfiguration)) {
//              /// 获取屏幕大小
//              with(composeWindow.graphicsConfiguration.bounds) {
//
//                IntRect(left = left, top = top, right = width - right, bottom = height - bottom)
//              }
//            }
//          }
        }
      }
    }
  }

  private val dialog = TaskbarDialog(taskbarController, taskbarDWebView.asDesktop())

  @Composable
  override fun TaskbarViewRender(draggableHelper: DraggableHelper, modifier: Modifier) {
    // TODO 将拖动反应到窗口位置上
    SideEffect {
      dialog.isVisible = true
    }
    taskbarController.state.composableHelper.apply {
      val layoutWidth by stateOf { layoutWidth }
      val layoutHeight by stateOf { layoutHeight }
      dialog.setSize(layoutWidth.toInt(), layoutHeight.toInt())
    }
  }

  @Composable
  override fun FloatWindow() {
    NormalFloatWindow()
  }

}