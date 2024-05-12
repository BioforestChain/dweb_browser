package org.dweb_browser.helper

import java.awt.Rectangle

fun PureRect.toAwtRectangle(times: Float) = timesToInt(times).toAwtRectangle()

fun PureIntRect.toAwtRectangle() = Rectangle(x, y, width, height)

fun Rectangle.toPureIntRect() = PureIntRect(x, y, width, height)