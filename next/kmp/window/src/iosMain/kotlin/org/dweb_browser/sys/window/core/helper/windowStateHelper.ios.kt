package org.dweb_browser.sys.window.core.helper

import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.sys.window.core.WindowController

actual suspend fun setDisplayMode(
  mode: DisplayMode?,
  win: WindowController
) {
  if (mode == DisplayMode.Fullscreen) {
    win.maximize()
  }
}