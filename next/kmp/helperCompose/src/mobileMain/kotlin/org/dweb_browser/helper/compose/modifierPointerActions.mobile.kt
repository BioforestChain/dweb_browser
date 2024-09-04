package org.dweb_browser.helper.compose

import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput

@Composable
actual fun Modifier.pointerActions(
  onHoverStart: () -> Unit,
  onHoverEnd: () -> Unit,
  onDoubleTap: () -> Unit,
  onMenu: () -> Unit,
  onTap: () -> Unit,
) = this.composed {
  val hoverStart by rememberUpdatedState(onHoverStart)
  val hoverEnd by rememberUpdatedState(onHoverEnd)
  val doubleTap by rememberUpdatedState(onDoubleTap)
  val openApp by rememberUpdatedState(onTap)
  val openAppMenu by rememberUpdatedState(onMenu)
  pointerInput(Unit) {
    detectTapGestures(
      onPress = {
        hoverStart()
      },
      onTap = {
        openApp()
        hoverEnd()
      },
      onLongPress = {
        openAppMenu()
        hoverEnd()
      },
      onDoubleTap = { doubleTap() },
    )
  }
    .pointerInput(Unit) {
      awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        hoverStart()
        val drag = awaitTouchSlopOrCancellation(down.id) { _, _ -> }
        if (drag == null) {
          hoverEnd()
        } else {
          awaitDragOrCancellation(drag.id)
          hoverEnd()
        }
      }
    }
}