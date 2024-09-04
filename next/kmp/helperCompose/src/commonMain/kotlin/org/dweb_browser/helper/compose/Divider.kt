package org.dweb_browser.helper.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Deprecated(
  "use material3 HorizontalDivider",
  replaceWith = ReplaceWith("androidx.compose.material3.HorizontalDivider()")
)
@Composable
fun HorizontalDivider(
  modifier: Modifier = Modifier,
  thickness: Dp = DividerDefaults.Thickness,
  color: Color = DividerDefaults.color,
) = Canvas(modifier.fillMaxWidth().height(thickness)) {
  drawLine(
    color = color,
    strokeWidth = thickness.toPx(),
    start = Offset(0f, thickness.toPx() / 2),
    end = Offset(size.width, thickness.toPx() / 2),
  )
}

@Deprecated(
  "use material3 VerticalDivider",
  replaceWith = ReplaceWith("androidx.compose.material3.VerticalDivider()")
)
@Composable
fun VerticalDivider(
  modifier: Modifier = Modifier,
  thickness: Dp = DividerDefaults.Thickness,
  color: Color = DividerDefaults.color,
) = Canvas(modifier.fillMaxHeight().width(thickness)) {
  drawLine(
    color = color,
    strokeWidth = thickness.toPx(),
    start = Offset(thickness.toPx() / 2, 0f),
    end = Offset(thickness.toPx() / 2, size.height),
  )
}