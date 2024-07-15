package org.dweb_browser.helper.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon

expect fun Modifier.hoverCursor(cursor: PointerIcon = PointerIcon.Hand): Modifier

expect val PointerIcon.Companion.HorizontalResize: PointerIcon
expect val PointerIcon.Companion.VerticalResize: PointerIcon
expect val PointerIcon.Companion.Move: PointerIcon
expect val PointerIcon.Companion.Wait: PointerIcon
