package org.dweb_browser.shared

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.dweb_browser.sys.shortcut.ShortcutManage
import org.dweb_browser.sys.shortcut.isScan
import platform.UIKit.UIApplicationShortcutItem

object DwebShortcutHandler {
  fun hand(shortcut: UIApplicationShortcutItem): Boolean {
    val mmid = shortcut.type
    val data = shortcut.userInfo?.get(mmid)
    MainScope().launch {
      dnsFetch("dweb://shortcutopen?mmid=${mmid}&data=${data}")
    }
    return true
  }

  fun isScanShortcut(shortcut: UIApplicationShortcutItem) = ShortcutManage.isScan(shortcut)
}