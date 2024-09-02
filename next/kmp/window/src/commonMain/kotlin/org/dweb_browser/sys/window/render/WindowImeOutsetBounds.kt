package org.dweb_browser.sys.window.render


import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp
import org.dweb_browser.sys.window.core.LocalWindowController
import org.dweb_browser.sys.window.core.LocalWindowsManager
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.watchedState
import org.dweb_browser.sys.window.core.constant.WindowManagerPropertyKeys
import org.dweb_browser.sys.window.helper.LocalWindowFrameStyle
import org.dweb_browser.sys.window.helper.watchedBounds
import org.dweb_browser.sys.window.helper.watchedState
import kotlin.math.max

@Composable
fun <T : WindowController> WindowsManager<T>.watchedImeBounds() =
  watchedState(WindowManagerPropertyKeys.ImeBoundingRect) { this.imeBoundingRect }

data class WindowImeOutsetBounds(
  /**
   * 键盘插入内容的高度
   */
  val keyboardInsetBottom: Float,
  /**
   * 窗口修正高度
   */
  val modifierOffsetY: Float,
)

@Composable
fun calcWindowImeOutsetBounds(
  wsm: WindowsManager<*> = LocalWindowsManager.current,
  win: WindowController = LocalWindowController.current,
): WindowImeOutsetBounds = runCatching {
  val modifierOffsetY: Float
  val keyboardInsetBottom: Float
  val imeVisible by wsm.watchedState { imeVisible }
  val isWinFocus by win.watchedState { focus }

  // 键盘不显示，或者窗口没聚焦
  if (!imeVisible || !isWinFocus) {
    modifierOffsetY = 0f
    keyboardInsetBottom = 0f
  } else {
    val winBounds by win.watchedBounds()
    val imeBounds by wsm.watchedImeBounds()
    val winBoundsBottom = winBounds.y + winBounds.height //

    if (winBoundsBottom <= imeBounds.y) {
      // 两个矩形没有交集 或者是全屏状态下
      modifierOffsetY = 0f
      keyboardInsetBottom = 0f
    } else { /// 尝试进行偏移修饰
      // 计算出需要偏移的量
      val requestOffsetY = winBoundsBottom - imeBounds.y
      // 窗口可以通过向上偏移来确保键盘与窗口同时显示
      if (requestOffsetY <= winBounds.y) {
        modifierOffsetY = -requestOffsetY
        keyboardInsetBottom = 0f
      } else {
        // 将顶部全部用作修正的量
        modifierOffsetY = -winBounds.y
        val winFrameStyle = LocalWindowFrameStyle.current
        // 窗口可以牺牲底部区域的显示，剩余的就是内容需要偏移的量
        val contentOffsetY = requestOffsetY - winFrameStyle.frameSize.bottom
        // 内容偏移量 加上 修正量，就是键盘的插入高度
        keyboardInsetBottom = max(contentOffsetY + modifierOffsetY, 0f)
      }
    }
  }
  WindowImeOutsetBounds(keyboardInsetBottom, modifierOffsetY)
}.getOrElse { WindowImeOutsetBounds(0f, 0f) }


fun Modifier.windowImeOutsetBounds(
  wsm: WindowsManager<*>,
  win: WindowController,
) = composed {
  val windowImeOutsetBounds = calcWindowImeOutsetBounds(wsm, win)

  win.state.keyboardInsetBottom = windowImeOutsetBounds.keyboardInsetBottom

  when (val modifierOffsetY = windowImeOutsetBounds.modifierOffsetY) {
    0f -> this
    else -> this.offset(y = modifierOffsetY.dp)
  }
}