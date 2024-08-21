package org.dweb_browser.sys.window.helper

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import org.dweb_browser.sys.window.core.WindowController

fun Modifier.windowTouchFocusable(win: WindowController): Modifier = this.pointerInput(win) {
  detectTapGestures(onPress = {
    win.focusInBackground()
  })
}