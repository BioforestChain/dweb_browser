package org.dweb_browser.browser.desk

import org.dweb_browser.helper.platform.PureViewController

class DesktopUIViewController(val vc: PureViewController) {
  init {
    DesktopViewControllerCore(vc)
  }
}