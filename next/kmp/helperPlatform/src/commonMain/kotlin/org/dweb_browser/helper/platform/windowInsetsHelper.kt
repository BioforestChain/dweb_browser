package org.dweb_browser.helper.platform

expect fun getCornerRadiusTop(
  viewController: IPureViewBox,
  density: Float,
  defaultValue: Float
): Float

expect fun getCornerRadiusBottom(
  viewController: IPureViewBox,
  density: Float,
  defaultValue: Float
): Float
