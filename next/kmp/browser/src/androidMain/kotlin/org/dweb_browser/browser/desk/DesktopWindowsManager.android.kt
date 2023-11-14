package org.dweb_browser.browser.desk

import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.IPlatformViewController
import org.dweb_browser.helper.platform.asAndroid

actual fun DesktopWindowsManager.Companion.getOrPutInstance(
  platformViewController: IPlatformViewController, onPut: (wm: DesktopWindowsManager) -> Unit
) = instances.getOrPut(platformViewController) {
  DesktopWindowsManager(platformViewController).also { dwm ->
    onPut(dwm)
    platformViewController.asAndroid().activity.onDestroyActivity {
      instances.remove(platformViewController)
    }
  }
}