package org.dweb_browser.sys.window.render

import org.dweb_browser.sys.window.core.WindowController

actual val WindowController.canOverlayNavigationBar get() = false

actual fun getWindowControllerBorderRounded(isMaximize: Boolean) =
  if (isMaximize) WindowPadding.CornerRadius.Zero else WindowPadding.CornerRadius.Default
