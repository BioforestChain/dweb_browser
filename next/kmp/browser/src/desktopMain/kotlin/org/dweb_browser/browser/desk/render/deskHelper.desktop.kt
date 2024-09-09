package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.dp


actual fun desktopGridLayout(): DesktopGridLayout = DesktopGridLayout(
  cells = GridCells.Adaptive(100.dp),
  insets = WindowInsets(top = 24.dp, left = 24.dp, right = 24.dp),
  horizontalSpace = 8.dp,
  verticalSpace = 16.dp,
)

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