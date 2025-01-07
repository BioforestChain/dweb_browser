package org.dweb_browser.helper.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.pointerActions(
  onHoverStart: (() -> Unit)?,
  onHoverEnd: (() -> Unit)?,
  onDoubleTap: (() -> Unit)?,
  onMenu: (() -> Unit)?,
  onTap: (() -> Unit)?,
) = this.onClick(
  matcher = PointerMatcher.mouse(PointerButton.Primary),
  onClick = onTap ?: {},
  onDoubleClick = onDoubleTap,
  onLongClick = onMenu,
).onClick(
  matcher = PointerMatcher.mouse(PointerButton.Secondary),
  onDoubleClick = onDoubleTap,
  onClick = onMenu ?: {},
).hoverEvent(onEnter = onHoverStart ?: {}, onExit = onHoverEnd ?: {}).hoverCursor()