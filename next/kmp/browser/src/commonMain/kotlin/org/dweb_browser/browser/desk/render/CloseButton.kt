package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity

@Composable
fun CloseButton(color: Color, modifier: Modifier = Modifier) {
  val lineWidth = 1 * LocalDensity.current.density
  Canvas(modifier.background(color.copy(alpha = 0.1f), CircleShape)) {
    val r = size.minDimension / 2.0f * 0.8f

    val xLeft = (size.width - 2 * r) / 2f + r * 0.7f
    val xRight = size.width - (size.width - 2 * r) / 2f - r * 0.7f
    val yTop = (size.height - 2 * r) / 2f + r * 0.7f
    val yBottom = size.height - (size.height - 2 * r) / 2f - r * 0.7f

    drawCircle(color.copy(alpha = 0.5f), r, style = Stroke(width = lineWidth))

    drawLine(
      color, Offset(xLeft, yTop), Offset(xRight, yBottom), lineWidth, cap = StrokeCap.Square
    )

    drawLine(
      color, Offset(xLeft, yBottom), Offset(xRight, yTop), lineWidth, cap = StrokeCap.Square
    )
  }
}