package org.dweb_browser.sys.tray.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.tray.TrayItem

actual suspend fun MicroModule.Runtime.registryTray(item: TrayItem): String {
  return item.id ?: ""
}