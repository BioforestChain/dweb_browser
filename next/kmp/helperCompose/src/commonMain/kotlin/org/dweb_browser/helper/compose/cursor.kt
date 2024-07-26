package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerIcon

expect fun Modifier.hoverCursor(cursor: PointerIcon = PointerIcon.Hand): Modifier
expect fun Modifier.hoverEvent(onEnter: () -> Unit, onExit: () -> Unit): Modifier
fun Modifier.hoverEvent(onHover: (Boolean) -> Unit) =
  hoverEvent(onEnter = { onHover(true) }, onExit = { onHover(false) })

fun Modifier.hoverComposed(onHover: @Composable Modifier.(Boolean) -> Modifier) = this.composed {
  var isHover by remember { mutableStateOf(false) }
  this.hoverEvent { isHover = it }.onHover(isHover)
}

expect val PointerIcon.Companion.HorizontalResize: PointerIcon
expect val PointerIcon.Companion.VerticalResize: PointerIcon
expect val PointerIcon.Companion.Move: PointerIcon
expect val PointerIcon.Companion.Wait: PointerIcon
