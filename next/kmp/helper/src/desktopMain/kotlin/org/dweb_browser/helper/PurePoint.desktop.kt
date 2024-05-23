package org.dweb_browser.helper


import java.awt.Point

fun PurePoint.toAwtPoint(times: Float) = timesToInt(times).toAwtPoint()

fun PureIntPoint.toAwtPoint() = Point(x, y)

fun Point.toPureIntPoint() = PureIntPoint(x, y)