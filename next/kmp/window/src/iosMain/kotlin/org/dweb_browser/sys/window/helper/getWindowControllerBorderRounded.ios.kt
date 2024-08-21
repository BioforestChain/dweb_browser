package org.dweb_browser.sys.window.helper

actual fun getWindowControllerBorderRounded(isMaximize: Boolean) =
  if (isMaximize) WindowFrameStyle.CornerRadius.Small else WindowFrameStyle.CornerRadius.Default
