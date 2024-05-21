package org.dweb_browser.browser.web.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

actual fun calculateGridsCell(
  pageSize: Int, maxWidth: Dp, maxHeight: Dp
): Triple<Int, Dp, Dp> {
  val count = maxWidth * 0.8f / 320.dp
  val cellCount = if(count < 1) 1 else count.toInt()

  if (count < 2) {
    val cellWidth = if (pageSize <= 1) {
      maxWidth * 0.618f
    } else {
      maxWidth * 0.8f / 2
    }

    return Triple(cellCount, cellWidth, cellWidth * (maxHeight * 1.0f / maxWidth))
  } else {
    return Triple(cellCount, 320.dp, 320.dp * (maxHeight * 1.0f / maxWidth))
  }
}
