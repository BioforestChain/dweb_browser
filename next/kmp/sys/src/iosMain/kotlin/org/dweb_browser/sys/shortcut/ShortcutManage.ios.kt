package org.dweb_browser.sys.shortcut

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.getUIApplication
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.toJsonElement
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.UIImage
import platform.UIKit.shortcutItems

private val maxCount = 4
private val scanMmid = "barcode-scanning.sys.dweb"
private val shortcutMmid = "shortcut.sys.dweb"

fun ShortcutManage.Companion.isScan(shortcut: UIApplicationShortcutItem): Boolean {
  return when (shortcut.type) {
    scanMmid -> true
    else -> false
  }
}

actual class ShortcutManage {

  companion object {}

  private val scope = CoroutineScope(Dispatchers.Main)

  actual suspend fun initShortcut() {

    scope.launch {
      val app = MicroModule.getUIApplication()
      val hasAddScan = app.shortcutItems?.firstOrNull { it ->
        val item = it as UIApplicationShortcutItem
        return@firstOrNull item.type == scanMmid
      } != null

      if (!hasAddScan) {
        app.shortcutItems = listOf(getScanShortcutItem())
      }
    }
  }

  actual suspend fun registryShortcut(shortcutList: List<SystemShortcut>): Boolean {
    scope.launch {
      val shortcuts = mutableListOf<UIApplicationShortcutItem>()
      shortcuts.add(getScanShortcutItem())
      shortcuts += shortcutList.map {
        getShortItem(it)
      }
      if (shortcuts.count() > maxCount) {
        shortcuts.add(maxCount-1, getMoreShortcutItem())
      }
      val app = MicroModule.getUIApplication()
      app.shortcutItems = shortcuts
    }
    return true
  }

  private fun getShortItem(item: SystemShortcut) : UIApplicationShortcutItem {
    return UIApplicationShortcutItem(item.mmid, item.title)
  }

  private fun getScanShortcutItem() : UIApplicationShortcutItem {
    val title = ShortcutI18nResource.default_qrcode_title.text
    return UIApplicationShortcutItem(scanMmid, title)
  }

  private fun getMoreShortcutItem() : UIApplicationShortcutItem {
    val title = ShortcutI18nResource.more_title.text
    return UIApplicationShortcutItem(shortcutMmid, title)
  }

  // iOS: 由于无法正确解析SVG图片，所以iOS会一直返回null.
  actual suspend fun getValidIcon(microModule: MicroModule, resource: ImageResource): ByteArray? {
    return null
  }
}