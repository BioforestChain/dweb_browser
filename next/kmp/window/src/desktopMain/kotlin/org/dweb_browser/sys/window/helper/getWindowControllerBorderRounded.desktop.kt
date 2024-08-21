package org.dweb_browser.sys.window.helper

import org.dweb_browser.helper.platform.PureViewController

actual fun getWindowControllerBorderRounded(isMaximize: Boolean) = when {
  PureViewController.isMacOS -> WindowFrameStyle.CornerRadius.Small.copy(topStart = 0f, topEnd = 0f)
  /**
   * Windows 操作系统背景不透明，所以始终为0
   */
  else -> WindowFrameStyle.CornerRadius.Zero
}


