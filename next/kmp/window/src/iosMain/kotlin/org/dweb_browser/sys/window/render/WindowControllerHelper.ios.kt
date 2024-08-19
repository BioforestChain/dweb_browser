package org.dweb_browser.sys.window.render

import org.dweb_browser.sys.window.core.WindowController

actual fun getWindowControllerBorderRounded(isMaximize: Boolean) =
  if (isMaximize) WindowPadding.CornerRadius.Small else WindowPadding.CornerRadius.Default
