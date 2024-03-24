package org.dweb_browser.helper

fun android.graphics.PointF.toPoint() = PurePoint(x, y)
fun android.graphics.Point.toPoint(density: Float = 1f) = PurePoint(x / density, y / density)


fun PurePoint.toAndroidPoint(density: Float = 1f) =
  android.graphics.Point((x * density).toInt(), (y * density).toInt())

fun PurePoint.toAndroidPointF() = android.graphics.PointF(x, y)
