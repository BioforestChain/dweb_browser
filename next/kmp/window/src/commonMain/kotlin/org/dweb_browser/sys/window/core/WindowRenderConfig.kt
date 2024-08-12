package org.dweb_browser.sys.window.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import org.dweb_browser.sys.window.render.WindowFrameStyle

internal class WindowRenderConfig {
  class FrameDragDelegate(
    private val onStart: ((startPoint: Offset) -> Unit)? = null,
    private val onEnd: (() -> Unit)? = null,
    private val onMove: ((change: PointerInputChange, dragAmount: Offset) -> Unit)? = null,
  ) {
    private var isMoving = false
    internal fun emitStart(startPoint: Offset) {
      if (!isMoving) {
        isMoving = true
        onStart?.invoke(startPoint)
      }
    }

    internal fun emitMove(change: PointerInputChange, dragAmount: Offset) {
      if (isMoving) {
        onMove?.invoke(change, dragAmount)
      }
    }

    internal fun emitEnd() {
      if (isMoving) {
        isMoving = false
        onEnd?.invoke()
      }
    }
  }

  var frameMoveDelegate by mutableStateOf<FrameDragDelegate?>(null)
  var frameLBResizeDelegate by mutableStateOf<FrameDragDelegate?>(null)
  var frameRBResizeDelegate by mutableStateOf<FrameDragDelegate?>(null)

  /**
   * 是否使用操作系统原生的窗口图层，同时用以提供 bounds
   */
  var isSystemWindow by mutableStateOf(false)

  /**
   * 是否使用 compose 绘制窗口边框
   */
  var isWindowUseComposeFrame by mutableStateOf(true)


  fun interface StyleWindowFrameDelegate {
    fun effectStyle(windowFrameStyle: WindowFrameStyle)
  }

  var styleWindowFrameDelegate by mutableStateOf<StyleWindowFrameDelegate?>(null)
}