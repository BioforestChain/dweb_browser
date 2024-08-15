package org.dweb_browser.helper

import java.awt.Rectangle

public fun PureRect.toAwtRectangle(times: Float): Rectangle = timesToInt(times).toAwtRectangle()

public fun PureIntRect.toAwtRectangle(): Rectangle = Rectangle(x, y, width, height)

public fun Rectangle.toPureIntRect(): PureIntRect = PureIntRect(x, y, width, height)