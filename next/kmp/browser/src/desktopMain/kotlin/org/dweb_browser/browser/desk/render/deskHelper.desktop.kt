package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.hoverCursor
import org.dweb_browser.helper.compose.hoverEvent
import org.mkdesklayout.project.NFCacalaterParams


actual fun desktopGridLayout(): DesktopGridLayout =
  DesktopGridLayout(
    cells = GridCells.Adaptive(100.dp),
    insets = WindowInsets(top = 24.dp, left = 24.dp, right = 24.dp),
    horizontalSpace = 8.dp,
    verticalSpace = 16.dp,
  )

@Composable
@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.desktopAppItemActions(
  onHoverStart: () -> Unit,
  onHoverEnd: () -> Unit,
  onDoubleTap: () -> Unit,
  onOpenApp: () -> Unit,
  onOpenAppMenu: () -> Unit,
) =
  this.onClick(
    matcher = PointerMatcher.mouse(PointerButton.Primary),
    onClick = onOpenApp,
    onDoubleClick = onDoubleTap,
    onLongClick = onOpenAppMenu,
  ).onClick(
    matcher = PointerMatcher.mouse(PointerButton.Secondary),
    onDoubleClick = onDoubleTap,
    onClick = onOpenAppMenu,
  ).hoverEvent(onEnter = onHoverStart, onExit = onHoverEnd).hoverCursor()

actual fun canSupportModifierBlur(): Boolean = true

actual fun getLayoutParams(width: Int, height: Int): NFCacalaterParams {
  val hSpace = 8
  val vSpace = 16
  val itemW = 100
  var column = width / (itemW + hSpace)
  var reminder = width - column * itemW - hSpace * (column - 1).coerceAtLeast(0)
  if (reminder > itemW) {
    column++
    reminder -= itemW
  }
  if (column < 4) {
    column = 4
    reminder = 0
  }
  return NFCacalaterParams(column, width - reminder, hSpace, vSpace, Pair(10, 12))
}

actual fun layoutSaveStrategyIsMultiple(): Boolean = false