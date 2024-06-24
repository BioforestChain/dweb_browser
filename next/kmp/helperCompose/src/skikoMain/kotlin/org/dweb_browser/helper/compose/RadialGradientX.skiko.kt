package org.dweb_browser.helper.compose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.skia.GradientStyle
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Shader

@Composable
actual fun RadialGradientX(
  modifier: Modifier,
  startX: Float,
  startY: Float,
  startRadius: Float,
  endX: Float,
  endY: Float,
  endRadius: Float,
  colors: Array<Color>,
  stops: Array<Float>?,
) {
  val paint = remember {
    Paint()
  }
  val d = LocalDensity.current.density
  remember(d, paint, startX, startY, startRadius, endX, endY, endRadius, colors, stops) {
    paint.shader = Shader.makeTwoPointConicalGradient(
      Point(startX * d, startY * d), startRadius * d, // Inner and outer point and radius
      Point(endX * d, endY * d), endRadius * d,
      colors.map { it.toArgb() }.toTypedArray().toIntArray(),
      positions = stops?.toFloatArray(),
      GradientStyle.DEFAULT,
    )
  }


  // Use Canvas composable to draw the gradient
  Canvas(modifier = modifier) {
    drawIntoCanvas { canvas ->
      canvas.nativeCanvas.drawRect(
        org.jetbrains.skia.Rect(0f, 0f, size.width, size.height),
        paint
      )
    }
  }
}