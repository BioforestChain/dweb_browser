package org.dweb_browser.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.sys.shortcut.ShortcutManage
import org.dweb_browser.sys.shortcut.isScan
import platform.UIKit.UIApplicationShortcutItem

object DwebShortcutHandler {
  fun hand(shortcut: UIApplicationShortcutItem): Boolean {
    val url = "file://desk.browser.dweb" + "/openAppOrActivate?app_id=" + shortcut.type
    CoroutineScope(Dispatchers.Main).launch {
      dnsFetch(url)
    }
    return true
  }

  fun isScanShortcut(shortcut: UIApplicationShortcutItem) = ShortcutManage.isScan(shortcut)
}