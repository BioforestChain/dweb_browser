package org.dweb_browser.helper.platform

import platform.Foundation.valueForKey
import platform.UIKit.UIScreen

actual fun getCornerRadiusTop(
  viewController: PlatformViewController, density: Float, defaultValue: Float
): Float {
  val cornerRadius = UIScreen.mainScreen.valueForKey("_displayCornerRadius")
  println("cornerRadius:$cornerRadius")
  return defaultValue
}

actual fun getCornerRadiusBottom(
  viewController: PlatformViewController, density: Float, defaultValue: Float
) = getCornerRadiusTop(viewController, density, defaultValue)
