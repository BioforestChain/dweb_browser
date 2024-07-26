package org.dweb_browser.helper.compose

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

actual fun Modifier.hoverCursor(cursor: PointerIcon): Modifier = pointerHoverIcon(cursor)

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.hoverEvent(onEnter: () -> Unit, onExit: () -> Unit): Modifier =
  this.onPointerEvent(PointerEventType.Enter) { onEnter() }
    .onPointerEvent(PointerEventType.Exit) { onExit() }

actual val PointerIcon.Companion.HorizontalResize by lazy { PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)) }
actual val PointerIcon.Companion.VerticalResize by lazy { PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR)) }
actual val PointerIcon.Companion.Move by lazy { PointerIcon(Cursor(Cursor.MOVE_CURSOR)) }
actual val PointerIcon.Companion.Wait by lazy { PointerIcon(Cursor(Cursor.WAIT_CURSOR)) }
