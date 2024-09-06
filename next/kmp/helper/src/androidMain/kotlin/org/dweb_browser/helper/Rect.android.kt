package org.dweb_browser.helper

import android.graphics.Rect
import android.graphics.RectF

public fun RectF.toRect(): PureRect =
  PureRect(x = left, y = top, width = width(), height = height())

public fun Rect.toRect(density: Float = 1f): PureRect = PureRect(
  x = left / density,
  y = top / density,
  width = width() / density,
  height = height() / density
)

public fun PureRect.toAndroidRect(density: Float = 1f): Rect =
  Rect(
    (x * density).toInt(),
    (y * density).toInt(),
    (x + width * density).toInt(),
    (y + height * density).toInt()
  )

public fun PureRect.toAndroidRectF(): RectF = RectF(x, y, x + width, y + height)
