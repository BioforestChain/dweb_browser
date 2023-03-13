package info.bagen.rust.plaoc.microService.helper

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import info.bagen.rust.plaoc.App

data class ColorJson(val red: Int, val blue: Int, val green: Int, val alpha: Int)

fun Color.toJsonAble(): ColorJson = convert(ColorSpaces.Srgb).let {
    ColorJson(it.red.toInt(), it.blue.toInt(), it.green.toInt(), it.alpha.toInt())
}

data class WebDomRect(val x: Float, val y: Float, val width: Float, val height: Float)

fun WindowInsets.toJsonAble(
    density: Density = Density(App.appContext),
    direction: LayoutDirection = LayoutDirection.Ltr
): WebDomRect {
    val left = this.getLeft(density, direction)
    val top = this.getTop(density)
    val right = this.getRight(density, direction)
    val bottom = this.getBottom(density)
    return WebDomRect(
        x = left.toFloat(),
        y = top.toFloat(),
        width = (right - left).toFloat(),
        height = (bottom - top).toFloat()
    )
}