package org.dweb_browser.helper

fun android.graphics.RectF.toRect() = PureRect(x = left, y = top, width = width(), height = height())
fun android.graphics.Rect.toRect(density: Float = 1f) = PureRect(
  x = left / density,
  y = top / density,
  width = width() / density,
  height = height() / density
)

fun PureRect.toAndroidRect(density: Float = 1f) =
  android.graphics.Rect(
    (x * density).toInt(),
    (y * density).toInt(),
    (x + width * density).toInt(),
    (y + height * density).toInt()
  )

fun PureRect.toAndroidRectF() = android.graphics.RectF(x, y, x + width, y + height)
