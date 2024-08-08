package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp


actual fun desktopGridLayout(): DesktopGridLayout =
  DesktopGridLayout(
    cells = GridCells.Adaptive(64.dp),
    insets = WindowInsets(left = 28.dp, right = 28.dp),
    horizontalSpace = 8.dp,
    verticalSpace = 16.dp,
  )

@Composable
actual fun Modifier.desktopAppItemActions(
  onHoverStart: () -> Unit,
  onHoverEnd: () -> Unit,
  onDoubleTap: () -> Unit,
  onOpenApp: () -> Unit,
  onOpenAppMenu: () -> Unit,
) = this.composed {
  val hoverStart by rememberUpdatedState(onHoverStart)
  val hoverEnd by rememberUpdatedState(onHoverEnd)
  val doubleTap by rememberUpdatedState(onDoubleTap)
  val openApp by rememberUpdatedState(onOpenApp)
  val openAppMenu by rememberUpdatedState(onOpenAppMenu)
  pointerInput(Unit) {
    detectTapGestures(
      onPress = {
        hoverStart()
      },
      onTap = {
        hoverEnd()
        openApp()
      },
      onLongPress = {
        hoverEnd()
        openAppMenu()
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
