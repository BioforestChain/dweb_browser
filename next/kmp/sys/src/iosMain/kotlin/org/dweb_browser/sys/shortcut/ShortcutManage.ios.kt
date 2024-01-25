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

  /**
   * 存储到系统文件，用于打开”更多“时，加载列表
   */
  actual suspend fun saveToSystemPreference(shortcutList: List<SystemShortcut>) {
    WARNING("Not yet implemented saveToSystemPreference")
  }
}