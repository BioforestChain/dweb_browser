package org.dweb_browser.sys.window.helper

actual fun getWindowControllerBorderRounded(isMaximize: Boolean) =
  if (isMaximize) WindowFrameStyle.CornerRadius.Zero else WindowFrameStyle.CornerRadius.Default
