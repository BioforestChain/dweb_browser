package org.dweb_browser.sys.window.render

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowRenderConfig


/**
 * 移动窗口的控制器
 */
internal fun Modifier.windowMoveAble(
  win: WindowController,
  frameDragDelegate: WindowRenderConfig.FrameDragDelegate? = win.state.renderConfig.frameDragDelegate,
) = this.pointerInput(win, frameDragDelegate) {
  /// 触摸窗口的时候，聚焦，并且提示可以移动
  detectTapGestures(
    // touchStart 的时候，聚焦移动
    onPress = {
      win.inMove.value = true
      frameDragDelegate?.emitStart(it)
      win.focusInBackground()
    },
    /// touchEnd 的时候，取消移动
    onTap = {
      win.inMove.value = false
      frameDragDelegate?.emitEnd()
    },
    onLongPress = {
      win.inMove.value = false
      frameDragDelegate?.emitEnd()
    },
  )
}.pointerInput(win, frameDragDelegate) {
  /// 拖动窗口
  detectDragGestures(
    onDragStart = {
      win.inMove.value = true
      frameDragDelegate?.emitStart(it)
      /// 开始移动的时候，同时进行聚焦
      win.focusInBackground()
    },
    onDragEnd = {
      win.inMove.value = false
      frameDragDelegate?.emitEnd()
    },
    onDragCancel = {
      win.inMove.value = false
      frameDragDelegate?.emitEnd()
    },
    onDrag = { change, dragAmount ->
      change.consume()
      frameDragDelegate?.emitMove(change, dragAmount)
    },
  )
}
