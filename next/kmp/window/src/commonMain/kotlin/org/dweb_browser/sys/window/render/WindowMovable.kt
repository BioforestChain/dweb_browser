package org.dweb_browser.sys.window.render

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.PureRect
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.renderConfig.FrameDragDelegate


/**
 * 移动窗口的控制器
 */
internal fun Modifier.windowMoveAble(
  win: WindowController,
  frameMoveDelegate: FrameDragDelegate? = win.state.renderConfig.frameMoveDelegate,
) = this.pointerInput(win, frameMoveDelegate) {
  /// 触摸窗口的时候，聚焦，并且提示可以移动
  detectTapGestures(
    // touchStart 的时候，聚焦移动
    onPress = {
      win.inMove.value = true
      frameMoveDelegate?.emitStart(it)
      win.focusInBackground()
    },
    /// touchEnd 的时候，取消移动
    onTap = {
      win.inMove.value = false
      frameMoveDelegate?.emitEnd()
    },
    onLongPress = {
      win.inMove.value = false
      frameMoveDelegate?.emitEnd()
    },
  )
}.pointerInput(win, frameMoveDelegate) {
  /// 拖动窗口
  detectDragGestures(
    onDragStart = {
      win.inMove.value = true
      frameMoveDelegate?.emitStart(it)
      /// 开始移动的时候，同时进行聚焦
      win.focusInBackground()
    },
    onDragEnd = {
      win.inMove.value = false
      frameMoveDelegate?.emitEnd()
    },
    onDragCancel = {
      win.inMove.value = false
      frameMoveDelegate?.emitEnd()
    },
    onDrag = { change, dragAmount ->
      frameMoveDelegate?.emitMove(change, dragAmount)
    },
  )
}


internal fun moveWindowBoundsInSafeBounds(
  winBounds: PureRect,
  safeBounds: PureBounds,
  moveAmount: Offset,
) = winBounds.mutable {
  var moveX = x + moveAmount.x
  var moveY = y + moveAmount.y
  if (moveX <= safeBounds.left) {
    moveX = safeBounds.left
  }
  if (moveX >= safeBounds.right) {
    moveX = safeBounds.right
  }
  x = moveX
  if (moveY <= safeBounds.top) {
    moveY = safeBounds.top
  }
  if (moveY >= safeBounds.bottom) {
    moveY = safeBounds.bottom
  }
  y = moveY
}
