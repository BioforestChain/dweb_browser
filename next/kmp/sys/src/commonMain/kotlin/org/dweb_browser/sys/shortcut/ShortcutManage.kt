package org.dweb_browser.sys.shortcut

expect class ShortcutManage() {
  /**
   * 一些默认的系统快捷入口配置
   */
  suspend fun initShortcut()

  /**
   * 动态注册的快捷列表
   */
  suspend fun registryShortcut(shortcutList: List<SystemShortcut>): Boolean
}