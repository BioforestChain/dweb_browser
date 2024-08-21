package org.dweb_browser.sys.window.render

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.renderConfig.FrameDragDelegate
import org.dweb_browser.sys.window.helper.WindowLimits
import kotlin.math.max

val inResizeStore = WeakHashMap<WindowController, MutableState<Boolean>>()

/** 窗口是否在调整大小中 */
val WindowController.inResize get() = inResizeStore.getOrPut(this) { mutableStateOf(false) }

/** 基于窗口左下角进行调整大小 */
internal fun Modifier.windowResizeByLeftBottom(
  win: WindowController,
  frameResizeDelegate: FrameDragDelegate? = win.state.renderConfig.frameLBResizeDelegate,
) = windowResize(win, frameResizeDelegate)

/** 基于窗口右下角进行调整大小 */
internal fun Modifier.windowResizeByRightBottom(
  win: WindowController,
  frameResizeDelegate: FrameDragDelegate? = win.state.renderConfig.frameRBResizeDelegate,
) = windowResize(win, frameResizeDelegate)

private fun Modifier.windowResize(
  win: WindowController,
  frameResizeDelegate: FrameDragDelegate?,
) = this.pointerInput(win, frameResizeDelegate) {
  var inResize by win.inResize
  detectDragGestures(
    onDragStart = {
      inResize = true
      frameResizeDelegate?.emitStart(it)
    },
    onDragEnd = {
      inResize = false
      frameResizeDelegate?.emitEnd()
    },
    onDragCancel = {
      inResize = false
      frameResizeDelegate?.emitEnd()
    },
    onDrag = { change, dragAmount ->
      frameResizeDelegate?.emitMove(change, dragAmount)
    },
  )
}


internal fun resizeWindowBoundsInLeftBottom(
  winBounds: PureRect,
  limits: WindowLimits,
  dragAmount: Offset,
) = winBounds.mutable {
  x += dragAmount.x
  width = max(width - dragAmount.x, limits.minWidth)
  height = max(height + dragAmount.y, limits.minHeight)
}

internal fun resizeWindowBoundsInRightBottom(
  winBounds: PureRect,
  limits: WindowLimits,
  dragAmount: Offset,
) = winBounds.mutable {
  width = max(width + dragAmount.x, limits.minWidth)
  height = max(height + dragAmount.y, limits.minHeight)
}
