package org.dweb_browser.helper.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

actual fun Modifier.cursorForHorizontalResize(): Modifier =
  pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))