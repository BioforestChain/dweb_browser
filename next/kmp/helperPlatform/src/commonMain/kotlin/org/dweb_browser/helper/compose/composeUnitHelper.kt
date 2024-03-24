package org.dweb_browser.helper.compose

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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


fun Rect.toSize() = Size(width, height)
fun IntRect.toIntSize() = IntSize(width, height)
