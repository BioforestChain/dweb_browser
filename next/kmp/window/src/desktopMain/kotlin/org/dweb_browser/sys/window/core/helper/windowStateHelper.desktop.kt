package org.dweb_browser.sys.window.core.helper

import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.sys.window.core.WindowController

actual suspend fun setDisplayMode(
  mode: DisplayMode?, win: WindowController
) {
  // 桌面端不对standalone 做最大化
  when (mode) {
    DisplayMode.Fullscreen -> win.fullscreen()
    else -> {}
  }
}