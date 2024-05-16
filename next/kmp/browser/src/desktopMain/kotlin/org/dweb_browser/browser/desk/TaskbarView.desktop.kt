package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.asDesktop
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.platform.getComposeWindowOrNull
import java.awt.Color
import java.awt.Dimension
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Shape
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JDialog
import javax.swing.Timer

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

    val contentView = taskbarDWebView.viewEngine.wrapperView
    private val parentWindow = taskbarController.deskNMM.getComposeWindowOrNull()

    private fun autoTaskbarPosition(parentBounds: Rectangle) {
      if (dragging) {
        return
      }
      val selfBounds = bounds
      val x = parentBounds.x + parentBounds.width + padding
      val y = parentBounds.y + parentBounds.height / 2 - selfBounds.height / 2
      if (x != selfBounds.x || y != selfBounds.y) {
        setLocation(x, y)
      }
    }

    private fun effectStyle() {
      val dialog = this
      dialog.isUndecorated = true; // 移除窗体装饰
      // 背景透明，这里的圆角和背景都将让 taskbar 内部自己绘制
      dialog.background = Color(0, 0, 0, 0)

      // 禁止焦点
      dialog.focusableWindowState = false
      dialog.isModal = false
      // 置顶
      dialog.isAlwaysOnTop = true

      dialog.add(dialog.contentView)
    }

    private fun effectRelativePosition() {
      val dialog = this
      // 位置
      dialog.parentWindow?.also { composeWindow ->
        dialog.addComponentListener(object : ComponentAdapter() {
          override fun componentResized(p0: ComponentEvent?) {
            dialog.autoTaskbarPosition(composeWindow.bounds)
          }
        })
        composeWindow.addComponentListener(object : ComponentAdapter() {
          override fun componentResized(p0: ComponentEvent?) {
            dialog.autoTaskbarPosition(composeWindow.bounds)
          }

          override fun componentMoved(p0: ComponentEvent?) {
            dialog.autoTaskbarPosition(composeWindow.bounds)
          }
        })

        // 初始化位置
        dialog.autoTaskbarPosition(composeWindow.bounds)
      }
      dialog.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseDragged(p0: MouseEvent) {
          println("QAQ mouseDragged ${p0.locationOnScreen}")
        }

        override fun mouseMoved(p0: MouseEvent) {
          println("QAQ mouseMoved ${p0.locationOnScreen}")
        }
      })
      dialog.addMouseListener(object : MouseAdapter() {
        override fun mouseReleased(p0: MouseEvent) {
          println("QAQ mouseReleased ${p0.locationOnScreen}")
          dragging = false
        }

        override fun mousePressed(p0: MouseEvent) {
          println("QAQ mousePressed ${p0.locationOnScreen}")
          dragging = false
        }

        override fun mouseClicked(p0: MouseEvent) {
          println("QAQ mouseClicked ${p0.locationOnScreen}")
          dragging = false
        }
      })
//      Timer(1000) {
//        println("QAQ $bounds ${contentView.bounds}")
//      }.start()
    }

    private var dragTimer: Timer? = null

    var dragging: Boolean
      set(value) {
        if (value == dragging) {
          return
        }
        if (value) {
          startDrag()
        } else {
          endDrag()
        }
      }
      get() = dragTimer != null

    private val currentMousePosition: Point?
      get() = MouseInfo.getPointerInfo().let { pointerInfo ->
        if (pointerInfo.device == graphicsConfiguration.device) {
          pointerInfo.location
        } else null
      }


    /**
     * 将一个矩形强制锁定跟随光标
     */
    private fun boxFollowMouse(
      boxLocation: Point,
      boxSize: Dimension,
      mousePoint: Point,
      safePadding: Int,
    ): Point {
      var startX = boxLocation.x
      var startY = boxLocation.y
      val contentLeft = boxLocation.x + safePadding
      val contentRight = boxLocation.x + boxSize.width - safePadding
      val contentTop = boxLocation.y + safePadding
      val contentBottom = boxLocation.y + boxSize.height - safePadding

      if (mousePoint.x < contentLeft) {
        startX = mousePoint.x - safePadding
      } else if (mousePoint.x > contentRight) {
        startX = mousePoint.x - boxSize.width + safePadding
      }
      if (mousePoint.y < contentTop) {
        startY = mousePoint.y - safePadding
      } else if (mousePoint.y > contentBottom) {
        startY = mousePoint.y - boxSize.height + safePadding
      }
      return Point(startX, startY)
    }

    private fun startDrag() {
      val dialog = this
      val startMousePoint = currentMousePosition ?: return endDrag()
      val dialogStartLocation = dialog.location
      val dialogStartSize = dialog.size

      /**
       * 这里需要校准位置，否则延迟会偏移问题
       */
      val contentStartLocation =
        boxFollowMouse(dialogStartLocation, dialogStartSize, startMousePoint, 10)
      val composeWindow = dialog.parentWindow
      val windowStartLocation = composeWindow?.location

      dialog.location = contentStartLocation

      dragTimer = Timer(10) {
        val moveMousePoint = currentMousePosition ?: return@Timer endDrag()
        val moveX = moveMousePoint.x - startMousePoint.x
        val moveY = moveMousePoint.y - startMousePoint.y
        val contentX = contentStartLocation.x + moveX
        val contentY = contentStartLocation.y + moveY
        if (contentX != dialog.x || contentY != dialog.y) {

          dialog.setLocation(contentStartLocation.x + moveX, contentStartLocation.y + moveY)

          println("QAQ MOVED dialogStartLocation=${dialogStartLocation} dialogStartSize=$dialogStartSize")
          println("QAQ MOVED contentStartLocation=${contentStartLocation} startMousePoint=$startMousePoint")
          println("QAQ MOVED location=${dialog.location} mousePoint=$currentMousePosition")

          if (windowStartLocation != null) {
            composeWindow.setLocation(
              windowStartLocation.x + moveX,
              windowStartLocation.y + moveY
            )
          }
        }

      }.also {
        it.start()
      }
    }

    private fun endDrag() {
      dragTimer?.stop()
      dragTimer = null

      // 结束后，强制执行一次定位
      parentWindow?.bounds?.also { autoTaskbarPosition(it) }
      if (taskbarController.state.desktopDragging) {
        taskbarController.state.desktopDragging = false
        /// 强行释放 js 里的状态
        taskbarDWebView.ioScope.launch(start = CoroutineStart.UNDISPATCHED) {
          runCatching {
            taskbarDWebView.evaluateAsyncJavascriptCode("dragEnd()")
          }
        }
      }
    }

    init {
      effectStyle()
      effectRelativePosition()
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
      val layoutWidth by stateOf { layoutWidth.toInt() }
      val layoutHeight by stateOf { layoutHeight.toInt() }
      val desktopDragging by stateOf { desktopDragging }
      dialog.setSize(layoutWidth, layoutHeight)
      dialog.dragging = desktopDragging
    }
  }

  @Composable
  override fun FloatWindow() {
    NormalFloatWindow()
  }

}