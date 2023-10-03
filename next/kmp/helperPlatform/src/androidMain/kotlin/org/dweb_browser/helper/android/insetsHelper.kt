package org.dweb_browser.helper.android

import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.Insets


//data class RectJson(val x: Float, val y: Float, val width: Float, val height: Float)
data class InsetsJson(val top: Float, val left: Float, val right: Float, val bottom: Float)

fun WindowInsets.toJsonAble(context: Context) = toJsonAble(Density(context))
fun WindowInsets.toJsonAble(
  density: Density, direction: LayoutDirection = LayoutDirection.Ltr
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
