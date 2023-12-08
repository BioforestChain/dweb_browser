package org.dweb_browser.helper

fun android.graphics.PointF.toPoint() = Point(x, y)
fun android.graphics.Point.toPoint(density: Float = 1f) = Point(x / density, y / density)


fun Point.toAndroidPoint(density: Float = 1f) =
  android.graphics.Point((x * density).toInt(), (y * density).toInt())

fun Point.toAndroidPointF() = android.graphics.PointF(x, y)
