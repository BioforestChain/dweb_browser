package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.dp
import org.mkdesklayout.project.NFCacalaterParams


actual fun desktopGridLayout(): DesktopGridLayout =
  DesktopGridLayout(
    cells = GridCells.Adaptive(64.dp),
    insets = WindowInsets(left = 28.dp, right = 28.dp),
    horizontalSpace = 8.dp,
    verticalSpace = 16.dp,
  )

actual fun getLayoutParams(width: Int, height: Int): NFCacalaterParams {
  val column = if (width > height) 8 else 4
  return NFCacalaterParams(column, width, 8, 16, Pair(10, 12))
}

actual fun layoutSaveStrategyIsMultiple(): Boolean = true