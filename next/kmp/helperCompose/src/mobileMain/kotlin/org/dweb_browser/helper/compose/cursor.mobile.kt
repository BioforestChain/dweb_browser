package org.dweb_browser.helper.compose

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput

actual fun Modifier.hoverCursor(cursor: PointerIcon): Modifier = this
actual fun Modifier.hoverEvent(onEnter: () -> Unit, onExit: () -> Unit): Modifier = this.composed {
  val currentOnEnter by rememberUpdatedState(onEnter)
  val currentOnExit by rememberUpdatedState(onExit)
  this.pointerInput(Unit) {
    detectTapGestures(
      onPress = {
        currentOnEnter()
        tryAwaitRelease()
        currentOnExit()
      }
    )
  }
}

actual val PointerIcon.Companion.HorizontalResize get() = PointerIcon.Default
actual val PointerIcon.Companion.VerticalResize get() = PointerIcon.Default
actual val PointerIcon.Companion.Move get() = PointerIcon.Default
actual val PointerIcon.Companion.Wait get() = PointerIcon.Default
