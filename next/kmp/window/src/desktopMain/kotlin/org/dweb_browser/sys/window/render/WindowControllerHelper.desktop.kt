package org.dweb_browser.sys.window.render

import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.core.WindowController

actual val WindowController.canOverlayNavigationBar: Boolean
  get() = false

/**
 * Windows 操作系统背景不透明，所以始终为0
 */
actual fun getWindowControllerBorderRounded(isMaximize: Boolean) =
  WindowPadding.CornerRadius.from(if (isMaximize || PureViewController.isWindows) 0 else 16)
