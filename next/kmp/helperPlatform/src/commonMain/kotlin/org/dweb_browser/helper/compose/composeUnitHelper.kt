package org.dweb_browser.helper.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

operator fun IntSize.div(value: Float) = Size(width / value, height / value)
operator fun IntRect.div(value: Float) =
  Rect(left / value, top / value, right / value, bottom / value)

fun Rect.timesToInt(value: Float) =
  IntRect(
    (left * value).toInt(),
    (top * value).toInt(),
    (right * value).toInt(),
    (bottom * value).toInt()
  )

fun Size.timesToInt(value: Float) =
  IntSize((width * value).toInt(), (height * value).toInt())

operator fun Size.minus(value: Size) = Size(width - value.width, height - value.height)
operator fun IntSize.minus(value: IntSize) = IntSize(width - value.width, height - value.height)
fun IntSize.minus(w: Int = 0, h: Int = 0) = IntSize(width - w, height - h)

fun IntRect.minus(l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) = IntRect(
  left = left - l, top = top - t, right = right - r, bottom = bottom - b,
)

fun Rect.toSize() = Size(width, height)
fun IntRect.toIntSize() = IntSize(width, height)

fun Offset.timesIntOffset(value: Float) = IntOffset((value * x).toInt(), (value * y).toInt())
fun IntOffset.divToFloat(value: Float) = Offset(x / value, y / value)
fun IntOffset.plus(x: Int, y: Int = 0) = IntOffset(this.x + x, this.y + y)
