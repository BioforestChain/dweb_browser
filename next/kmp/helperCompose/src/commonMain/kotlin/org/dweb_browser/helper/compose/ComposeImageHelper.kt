package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
fun rememberVectorPainterWithTint(
  image: ImageVector,
  tintBlendMode: BlendMode? = null,
  tintColor: Color? = null,
) =
  rememberVectorPainter(
    defaultWidth = image.defaultWidth,
    defaultHeight = image.defaultHeight,
    viewportWidth = image.viewportWidth,
    viewportHeight = image.viewportHeight,
    name = image.name,
    tintColor = tintColor ?: image.tintColor,
    tintBlendMode = tintBlendMode ?: image.tintBlendMode,
    autoMirror = image.autoMirror,
    content = { _, _ -> RenderVectorGroup(group = image.root) }
  )

//val shapeCache = mutableMapOf<Float, GenericShape>()

//fun SimpleSquircleShape(r: Float = 0.445f) = shapeCache.getOrPut(r) {
//  GenericShape { size, _ ->
//    with(size) {
//      val halfW = width / 2
//      val halfH = height / 2
//      val squircleW = halfW * r
//      val squircleH = halfH * r
//      val zeroX = 0f
//      val zeroY = 0f
//      val nSquircleW = width - squircleW
//      val nSquircleH = height - squircleH
//      moveTo(zeroX, halfH)
//      cubicTo(zeroX, squircleH, squircleW, zeroY, halfW, zeroY)
//      cubicTo(nSquircleW, zeroY, width, squircleH, width, halfH)
//      cubicTo(width, nSquircleH, nSquircleW, width, halfW, height)
//      cubicTo(squircleW, height, zeroX, nSquircleH, zeroX, halfH)
//      close()
//    }
//  }
//}