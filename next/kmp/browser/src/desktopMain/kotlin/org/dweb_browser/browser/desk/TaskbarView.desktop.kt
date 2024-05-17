package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.file.ext.store
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.asDesktop
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.PureIntPoint
import org.dweb_browser.helper.platform.getAnimationFrameMs
import org.dweb_browser.helper.platform.getComposeWindowOrNull
import org.dweb_browser.helper.platform.getScreenBounds
import org.dweb_browser.helper.toPureIntPoint
import java.awt.Color
import java.awt.Dimension
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Shape
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JDialog
import javax.swing.Timer
import kotlin.math.max

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

    private var ready = false

    override fun setLocation(p0: Int, p1: Int) {
      val current = location
      if (current.x == p0 && current.y == p1) {
        return
      }

      super.setLocation(p0, p1)
      if (ready && !taskbarMagnetEffect.isPlay) {
        playMagnetEffect()
      }
    }

    override fun setSize(p0: Int, p1: Int) {
      val current = size
      if (current.width == p0 && current.height == p1) {
        return
      }
      super.setSize(p0, p1)
      if (ready && !taskbarMagnetEffect.isPlay) {
        playMagnetEffect()
      }
    }

    private fun playMagnetEffect() {
      taskbarMagnetEffect.start()
    }

    /**
     * taskbar 吸附在屏幕边缘
     */
    private val taskbarMagnetEffect = MagnetEffect(
      from = this,
      isDisabled = { dragging },
      getEnds = {
        val viewBounds = bounds
        val screenBounds = getScreenBounds()
        val paddingX = padding
        val paddingY = padding
        val centerX = viewBounds.x + viewBounds.width / 2
        println("QAQ viewBounds=$viewBounds parentBounds=$screenBounds")
        val endX = when {
          // 往右靠边
          centerX - screenBounds.left > screenBounds.right - centerX -> {
            screenBounds.right - viewBounds.width - paddingX
          }
          // 往左靠边
          else -> {
            screenBounds.left + paddingX
          }
        }
        val endY = when {
          // 如果上方溢出
          viewBounds.y < screenBounds.top -> screenBounds.top + paddingY
          // 如果下方溢出
          viewBounds.y + viewBounds.height > screenBounds.bottom -> screenBounds.bottom - viewBounds.height - paddingY
          // 保持
          else -> viewBounds.y
        }
        PureIntPoint(endX, endY)
      },
    )

    /**
     * 桌面吸附在 taskbar 边缘
     */
    private val desktopMagnetEffect = parentWindow?.let { composeWindow ->
      MagnetEffect(
        from = composeWindow,
        isDisabled = { taskbarMagnetEffect.isPlay },
        getEnds = {
          val windowBounds = composeWindow.bounds
          val taskbarBounds = bounds
          val taskbarCenterX = taskbarBounds.x + taskbarBounds.width / 2
          val taskbarCenterY = taskbarBounds.y + taskbarBounds.height / 2
          val screenBounds = getScreenBounds()
          val screenCenterX = (screenBounds.right - screenBounds.left) / 2 + screenBounds.left

          val endX = when {
            // 屏幕左边空间更多
            taskbarCenterX > screenCenterX -> taskbarBounds.x - padding - windowBounds.width
            // 屏幕右边空间更多
            else -> taskbarBounds.x + taskbarBounds.width + padding
          }

          /// y轴比较宽松，只要有部分交集就好了
          var endY = when {
            // window 顶部低于 taskbar 的中心
            windowBounds.y > taskbarCenterY -> taskbarCenterY
            // window 底部高于 taskbar 的中心
            windowBounds.y + windowBounds.height < taskbarCenterY -> taskbarCenterY - windowBounds.height
            else -> windowBounds.y
          }
          // 尽量不要溢出屏幕，优先确保顶部显示
          if (endY < screenBounds.top) {
            endY = screenBounds.top
          } else if (endY + windowBounds.height > screenBounds.bottom) {
            endY = screenBounds.bottom - windowBounds.height
          }

          PureIntPoint(endX, endY)
        },
      ).also {
        taskbarMagnetEffect.onPlayEnd = {
          it.start()
        }
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
      dialog.taskbarController.deskNMM.also { nmm ->
        nmm.scopeLaunch(cancelable = true) {
          var location = nmm.store.getOrPut("taskbar.location") {
            getScreenBounds().let {
              PureIntPoint(
                it.right - 100, (it.bottom - it.top) / 2 + it.top - 100
              )
            }
          }
          dialog.setLocation(location.x, location.y)
          // 初始化动画
          playMagnetEffect()
          desktopMagnetEffect?.start()

          while (true) {
            delay(2000)
            dialog.location.toPureIntPoint().also {
              if (location != it) {
                location = it
                nmm.store.set("taskbar.location", it)
              }
            }
          }
        }
      }
      dialog.addMouseListener(object : MouseAdapter() {
        override fun mouseReleased(p0: MouseEvent) {
          dragging = false
        }

        override fun mousePressed(p0: MouseEvent) {
          dragging = false
        }

        override fun mouseClicked(p0: MouseEvent) {
          dragging = false
        }
      })
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
      safePaddingX: Int,
      safePaddingY: Int,
    ): Point {
      var startX = boxLocation.x
      var startY = boxLocation.y
      val contentLeft = boxLocation.x + safePaddingX
      val contentRight = boxLocation.x + boxSize.width - safePaddingX
      val contentTop = boxLocation.y + safePaddingY
      val contentBottom = boxLocation.y + boxSize.height - safePaddingY

      if (mousePoint.x < contentLeft) {
        startX = mousePoint.x - safePaddingX
      } else if (mousePoint.x > contentRight) {
        startX = mousePoint.x - boxSize.width + safePaddingX
      }
      if (mousePoint.y < contentTop) {
        startY = mousePoint.y - safePaddingY
      } else if (mousePoint.y > contentBottom) {
        startY = mousePoint.y - boxSize.height + safePaddingY
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
      val contentStartLocation = boxFollowMouse(
        dialogStartLocation,
        dialogStartSize,
        startMousePoint,
        /// css 写的是 scale(0.9)，所以这边确保鼠标在其内部
        max((dialogStartSize.width * 0.1).toInt(), 10),
        max((dialogStartSize.height * 0.1).toInt(), 10),
      )
      val composeWindow = dialog.parentWindow

      dialog.location = contentStartLocation

      dragTimer = Timer(getAnimationFrameMs()) {
        val moveMousePoint = currentMousePosition ?: return@Timer endDrag()
        val moveX = moveMousePoint.x - startMousePoint.x
        val moveY = moveMousePoint.y - startMousePoint.y
        val contentX = contentStartLocation.x + moveX
        val contentY = contentStartLocation.y + moveY
        if (contentX != dialog.x || contentY != dialog.y) {
          dialog.setLocation(contentStartLocation.x + moveX, contentStartLocation.y + moveY)
        }

      }.also {
        it.start()
      }
    }

    private fun endDrag() {
      dragTimer?.stop()
      dragTimer = null

      if (taskbarController.state.taskbarDragging) {
        taskbarController.state.taskbarDragging = false
        /// 强行释放 js 里的状态
        taskbarDWebView.ioScope.launch(start = CoroutineStart.UNDISPATCHED) {
          runCatching {
            taskbarDWebView.evaluateAsyncJavascriptCode("dragEnd()")
          }
        }
      }
      // 结束后，强制执行复位动画
      playMagnetEffect()
    }

    init {
      effectStyle()
      effectRelativePosition()
      ready = true
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
      val taskbarDragging by stateOf { taskbarDragging }
      dialog.setSize(layoutWidth, layoutHeight)
      dialog.dragging = taskbarDragging
    }
  }

  @Composable
  override fun FloatWindow() {
    NormalFloatWindow()
  }

}