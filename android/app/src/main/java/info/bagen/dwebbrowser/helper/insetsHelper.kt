package info.bagen.dwebbrowser.helper

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.Insets
import info.bagen.dwebbrowser.App


//data class RectJson(val x: Float, val y: Float, val width: Float, val height: Float)
data class InsetsJson(val top: Float, val left: Float, val right: Float, val bottom: Float)

fun WindowInsets.toJsonAble(
  density: Density = Density(App.appContext), direction: LayoutDirection = LayoutDirection.Ltr
) = InsetsJson(
  top = getTop(density).toFloat(),
  left = getLeft(density, direction).toFloat(),
  right = getRight(density, direction).toFloat(),
  bottom = getBottom(density).toFloat(),
)

fun Insets.toJsonAble() = InsetsJson(
  top = top.toFloat(),
  left = left.toFloat(),
  right = right.toFloat(),
  bottom = bottom.toFloat(),
)
