package org.dweb_browser.helper.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.hoverCursor(cursor: PointerIcon): Modifier = composed {
  var isHovering by remember { mutableStateOf(false) }
  this.onPointerEvent(PointerEventType.Enter) { isHovering = true }
    .onPointerEvent(PointerEventType.Exit) { isHovering = false }
    .pointerHoverIcon(if (isHovering) cursor else PointerIcon.Default)
}

actual val PointerIcon.Companion.HorizontalResize by lazy { PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)) }
actual val PointerIcon.Companion.VerticalResize by lazy { PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR)) }
actual val PointerIcon.Companion.Move by lazy { PointerIcon(Cursor(Cursor.MOVE_CURSOR)) }
actual val PointerIcon.Companion.Wait by lazy { PointerIcon(Cursor(Cursor.WAIT_CURSOR)) }
