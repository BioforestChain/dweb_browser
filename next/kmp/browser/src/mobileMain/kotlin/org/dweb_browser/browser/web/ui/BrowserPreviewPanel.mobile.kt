package org.dweb_browser.browser.web.ui

import androidx.compose.ui.unit.Dp

actual fun calculateGridsCell(
  pageSize: Int, maxWidth: Dp, maxHeight: Dp
): Triple<Int, Dp, Dp> {
  val count = if (pageSize <= 1) 1 else 2

  val cellWidth = when (count == 1) {
    true -> maxWidth * 0.618f
    else -> maxWidth * 0.8f / 2
  }

  return Triple(count, cellWidth, cellWidth * (maxHeight * 1.0f / maxWidth))
}
