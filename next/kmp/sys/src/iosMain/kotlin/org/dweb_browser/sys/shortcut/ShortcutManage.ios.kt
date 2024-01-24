package org.dweb_browser.sys.shortcut

import org.dweb_browser.helper.WARNING

actual class ShortcutManage {
  actual suspend fun initShortcut() {
    WARNING("Not yet implemented initShortcut")
  }

  actual suspend fun registryShortcut(shortcutList: List<SystemShortcut>): Boolean {
    WARNING("Not yet implemented registryShortcut")
    return false
  }
}