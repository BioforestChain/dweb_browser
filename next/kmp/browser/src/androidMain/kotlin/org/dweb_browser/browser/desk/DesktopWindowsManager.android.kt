package org.dweb_browser.browser.desk

import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.asAndroid

actual fun TabletopWindowsManager.Companion.getOrPutInstance(
  platformViewController: IPureViewController,
  viewBox: IPureViewBox,
  onPut: (wm: TabletopWindowsManager) -> Unit,
) = instances.getOrPut(platformViewController) {
  TabletopWindowsManager(platformViewController, viewBox).also { dwm ->
    onPut(dwm)
    platformViewController.asAndroid().onDestroyActivity {
      instances.remove(platformViewController)
    }
  }
}
