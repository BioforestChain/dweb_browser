package org.dweb_browser.browser.desk

import kotlinx.coroutines.launch
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.IPlatformViewController
import org.dweb_browser.helper.platform.asIos

actual  fun DesktopWindowsManager.Companion.getOrPutInstance(
  platformViewController: IPlatformViewController,
  onPut: (wm: DesktopWindowsManager) -> Unit
) = instances.getOrPut(platformViewController) {
  DesktopWindowsManager(platformViewController).also { dwm ->
    onPut(dwm)
    /// TODO 需要有生命周期钩子来确保它能被移除，避免内存溢出
    platformViewController.lifecycleScope.launch {
//      platformViewController.asIos().uiViewController().viewDid
    }
//    instances.remove(platformViewController.platformContext)
  }
}
