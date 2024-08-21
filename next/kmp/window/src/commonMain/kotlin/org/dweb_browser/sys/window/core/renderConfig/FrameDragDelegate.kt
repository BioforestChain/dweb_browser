package org.dweb_browser.sys.window.core.renderConfig

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange

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
