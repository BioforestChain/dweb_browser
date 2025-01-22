package org.dweb_browser.sys.window.render

import androidx.compose.ui.unit.Density
import org.dweb_browser.helper.android.NavigationBarUtil
import org.dweb_browser.helper.getAppContextUnsafe

actual fun getVirtualNavigationBarHeight(): Float {
  val virtualNavigationBarHeight = NavigationBarUtil.getNavigationBarHeight(getAppContextUnsafe())
  val displayMetrics = getAppContextUnsafe().resources.displayMetrics
  return virtualNavigationBarHeight / displayMetrics.density
}
