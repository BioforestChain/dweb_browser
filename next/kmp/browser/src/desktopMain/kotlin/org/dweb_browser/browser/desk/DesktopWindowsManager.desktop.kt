package org.dweb_browser.browser.desk

import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController

actual fun TabletopWindowsManager.Companion.getOrPutInstance(
  platformViewController: IPureViewController,
  viewBox: IPureViewBox,
  onPut: (wm: TabletopWindowsManager) -> Unit,
) = instances.getOrPut(platformViewController) {
  TabletopWindowsManager(platformViewController, viewBox).also { dwm ->
    onPut(dwm)
    platformViewController.onDestroy {
      instances.remove(platformViewController)
    }
  }
}
