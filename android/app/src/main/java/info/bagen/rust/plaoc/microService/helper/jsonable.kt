package info.bagen.rust.plaoc.microService.helper

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.Insets
import info.bagen.rust.plaoc.App

data class ColorJson(val red: Int, val blue: Int, val green: Int, val alpha: Int) {
    fun toColor() = Color(red = red, blue = blue, green = green, alpha = alpha)
}

fun Color.toJsonAble(): ColorJson = convert(ColorSpaces.Srgb).let {
    ColorJson(
        (it.red * 255).toInt(),
        (it.blue * 255).toInt(),
        (it.green * 255).toInt(),
        (it.alpha * 255).toInt()
    )
}

data class RectJson(val x: Float, val y: Float, val width: Float, val height: Float)
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
