package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun CloseButton(modifier: Modifier) {
  Canvas(modifier.background(Color.Black.copy(alpha = 0.1f), CircleShape)) {
    val r = size.minDimension / 2.0f * 0.8f

    val xLeft = (size.width - 2 * r) / 2f + r * 0.7f
    val xRight = size.width - (size.width - 2 * r) / 2f - r * 0.7f
    val yTop = (size.height - 2 * r) / 2f + r * 0.7f
    val yBottom = size.height - (size.height - 2 * r) / 2f - r * 0.7f

    val lineWidth = taskBarCloseButtonLineWidth()

    drawCircle(Color.Gray, r, style = Stroke(width = lineWidth))

    drawLine(
      Color.Black, Offset(xLeft, yTop), Offset(xRight, yBottom), lineWidth
    )

    drawLine(
      Color.Black, Offset(xLeft, yBottom), Offset(xRight, yTop), lineWidth
    )
  }
}