package org.dweb_browser.browser.desk

import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.asDesktop

actual fun DesktopWindowsManager.Companion.getOrPutInstance(
  platformViewController: IPureViewController,
  viewBox: IPureViewBox,
  onPut: (wm: DesktopWindowsManager) -> Unit
) = instances.getOrPut(platformViewController) {
  DesktopWindowsManager(platformViewController, viewBox).also { dwm ->
    onPut(dwm)
    platformViewController.onDestroy {
      instances.remove(platformViewController)
    }
  }
}

actual fun DesktopWindowsManager.focusPlatformDesktop() {
  viewController.asDesktop().getComposeWindowOrNull()?.toFront()
}
