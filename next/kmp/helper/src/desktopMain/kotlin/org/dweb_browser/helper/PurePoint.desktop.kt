package org.dweb_browser.helper


import java.awt.Point

public fun PurePoint.toAwtPoint(times: Float): Point = timesToInt(times).toAwtPoint()

public fun PureIntPoint.toAwtPoint(): Point = Point(x, y)

public fun Point.toPureIntPoint(): PureIntPoint = PureIntPoint(x, y)