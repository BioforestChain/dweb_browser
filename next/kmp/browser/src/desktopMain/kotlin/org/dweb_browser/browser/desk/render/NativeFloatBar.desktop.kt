package org.dweb_browser.browser.desk.render

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.WindowPlacement
import kotlinx.coroutines.delay
import org.dweb_browser.browser.desk.MagnetEffect
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.store
import org.dweb_browser.helper.PureIntPoint
import org.dweb_browser.helper.PureIntRect
import org.dweb_browser.helper.platform.getAnimationFrameMs
import org.dweb_browser.helper.platform.getScreenBounds
import org.dweb_browser.helper.toPureIntPoint
import org.dweb_browser.helper.toPureIntRect
import org.dweb_browser.sys.window.floatBar.FloatBarState
import java.awt.Color
import java.awt.Dimension
import java.awt.MouseInfo
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.Timer
import kotlin.math.max

abstract class NativeFloatBarContent(val component: JComponent) {
  open fun onEndDrag() {}
}

/**
 * ## 磁吸浮动条
 * 配合原生窗口的原生 FloatBar
 */
internal class NativeMagnetFloatBar(
  val state: FloatBarState,
  val runtime: NativeMicroModule.NativeRuntime,
  val content: NativeFloatBarContent,
  val parentWindow: ComposeWindow,
) : JDialog() {
  val round = 20
  val padding = 5

  private var ready = false

  override fun setLocation(p0: Int, p1: Int) {
    val current = location
    if (current.x == p0 && current.y == p1) {
      return
    }

    /// 记录 taskbar 的横向坐标，用于判断是沉浸式时将taskbar隐藏于哪一侧
    if(ready) {
      state.offsetXFlow.value = p0
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

  internal fun playMagnetEffect() {
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
  private val desktopMagnetEffect = parentWindow.let { composeWindow ->
    val paddingY = 20
    val paddingX = 20
    MagnetEffect(
      from = composeWindow,
      isDisabled = {
        taskbarMagnetEffect.isPlay || composeWindow.placement == WindowPlacement.Maximized || composeWindow.placement == WindowPlacement.Fullscreen
      },
      getEnds = {
        val windowBounds = composeWindow.bounds
        val taskbarBounds = bounds
        val taskbarCenterX = taskbarBounds.x + taskbarBounds.width / 2
        val taskbarCenterY = taskbarBounds.y + taskbarBounds.height / 2
        val screenBounds = getScreenBounds()
        if ((windowBounds.height + paddingY) > screenBounds.height) {
          windowBounds.height = screenBounds.height - paddingY
        }
        if ((windowBounds.width + paddingX + taskbarBounds.width) > screenBounds.width) {
          windowBounds.width = screenBounds.width - paddingX - taskbarBounds.width
        }
        composeWindow.bounds = windowBounds

        val screenCenterX = (screenBounds.right - screenBounds.left) / 2 + screenBounds.left
        val endX = when {
          // 屏幕左边空间更多, 如果windowBounds.width > taskbar.x - padding，例如全屏，则 endX = 0 不该左移，否则会溢出屏幕
          taskbarCenterX > screenCenterX -> (taskbarBounds.x - padding - windowBounds.width).let { if (it > 0) it else 0 }
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
    dialog.isUndecorated = true // 移除窗体装饰
    // 背景透明，这里的圆角和背景都将让 taskbar 内部自己绘制
    dialog.background = Color(0, 0, 0, 0)

    // 禁止焦点
    dialog.focusableWindowState = false
    dialog.isModal = false
    // 置顶
    dialog.isAlwaysOnTop = true

    add(content.component)
  }

  private fun effectRelativePosition() {
    val dialog = this
    // 位置
    dialog.runtime.also { nmm ->
      nmm.scopeLaunch(cancelable = true) {
        var taskbarLocation = nmm.store.getOrPut("taskbar.location") {
          getScreenBounds().let {
            PureIntPoint(
              it.right - 100, (it.bottom - it.top) / 2 + it.top - 100
            )
          }
        }.apply {
          dialog.setLocation(x, y)
        }

        var desktopBounds = nmm.store.getOrNull<PureIntRect>("desktop.bounds")?.apply {
          parentWindow.setBounds(x, y, width, height)
        }

        // 初始化动画
        playMagnetEffect()
        desktopMagnetEffect.start()

        while (true) {
          delay(2000)
          dialog.location.toPureIntPoint().also {
            if (taskbarLocation != it) {
              taskbarLocation = it
              nmm.store.set("taskbar.location", it)
            }
          }
          parentWindow.apply {
            bounds.toPureIntRect().also {
              if (desktopBounds != it) {
                desktopBounds = it
                nmm.store.set("desktop.bounds", it)
              }
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

    if (state.dragging) {
      state.dragging = false
      content.onEndDrag()
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
