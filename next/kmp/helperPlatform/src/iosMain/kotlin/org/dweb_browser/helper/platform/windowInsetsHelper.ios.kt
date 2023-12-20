package org.dweb_browser.helper.platform

import platform.Foundation.valueForKey
import platform.UIKit.UIScreen

actual fun getCornerRadiusTop(
  viewController: IPureViewBox, density: Float, defaultValue: Float
): Float {
  val cornerRadius = UIScreen.mainScreen.valueForKey("_displayCornerRadius")
  if (cornerRadius is Double) {
    return cornerRadius.toFloat() / 2f
  }
  return defaultValue
}

actual fun getCornerRadiusBottom(
  viewController: IPureViewBox, density: Float, defaultValue: Float
) = getCornerRadiusTop(viewController, density, defaultValue)
