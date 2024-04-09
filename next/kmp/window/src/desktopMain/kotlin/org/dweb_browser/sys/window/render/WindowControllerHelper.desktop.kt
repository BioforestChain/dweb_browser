package org.dweb_browser.sys.window.render

import org.dweb_browser.sys.window.core.WindowController

actual val WindowController.canOverlayNavigationBar: Boolean
  get() = false

/**
 * Windows 操作系统背景不透明，所以始终为0
 */
actual fun getWindowControllerBorderRounded(isMaximize: Boolean) =
  WindowPadding.CornerRadius.from(0) //始终设置为0，桌面端设置16的圆角不好看

// if (isMaximize || PureViewController.isWindows) 0 else 16
