package info.bagen.rust.plaoc.microService.helper

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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

fun WindowInsets.toJsonAble(
    density: Density = Density(App.appContext),
    direction: LayoutDirection = LayoutDirection.Ltr
): RectJson {
    val left = this.getLeft(density, direction)
    val top = this.getTop(density)
    val right = this.getRight(density, direction)
    val bottom = this.getBottom(density)
    return RectJson(
        x = left.toFloat(),
        y = top.toFloat(),
        width = (right - left).toFloat(),
        height = (bottom - top).toFloat()
    )
}