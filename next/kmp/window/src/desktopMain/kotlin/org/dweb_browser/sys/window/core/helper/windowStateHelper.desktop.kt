package org.dweb_browser.sys.window.core.helper

import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.sys.window.core.WindowController

actual suspend fun setDisplayMode(
  mode: DisplayMode?,
  win: WindowController
) {
  // TODO 桌面端窗口不做默认全屏，这里可能会引入参数应用类型做进一步判断
}