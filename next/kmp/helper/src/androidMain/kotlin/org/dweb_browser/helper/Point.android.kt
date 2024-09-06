package org.dweb_browser.helper

import android.graphics.Point
import android.graphics.PointF

public fun android.graphics.PointF.toPoint(): PurePoint = PurePoint(x, y)
public fun android.graphics.Point.toPoint(density: Float = 1f): PurePoint =
  PurePoint(x / density, y / density)


public fun PurePoint.toAndroidPoint(density: Float = 1f): Point =
  android.graphics.Point((x * density).toInt(), (y * density).toInt())

public fun PurePoint.toAndroidPointF(): PointF = PointF(x, y)
