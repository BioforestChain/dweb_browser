package org.dweb_browser.sys.shortcut

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.getUIApplication
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.withScope
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationShortcutIcon
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.shortcutItems

private const val maxCount = 4
private const val scanMmid = "barcode-scanning.sys.dweb"
private const val shortcutMmid = "shortcut.sys.dweb"

fun ShortcutManage.Companion.isScan(shortcut: UIApplicationShortcutItem): Boolean {
  return when (shortcut.type) {
    scanMmid -> true
    else -> false
  }
}

actual class ShortcutManage {

  companion object {}

  private val mainScope = CoroutineScope(Dispatchers.Main)
  lateinit var app: UIApplication
  actual suspend fun initShortcut(microModule: MicroModule.Runtime) {
    withScope(mainScope) {
      app = microModule.getUIApplication()
      // 这里获取的都是动态的quick action
      val hasAddScan = app.shortcutItems?.firstOrNull {
        val item = it as UIApplicationShortcutItem
        return@firstOrNull item.type == scanMmid
      } != null
      // 没有添加过扫码的就添加一下
      if (!hasAddScan) {
        app.shortcutItems = listOf(getScanShortcutItem())
      }
    }
  }

  actual suspend fun registryShortcut(shortcutList: List<SystemShortcut>): Boolean {
    mainScope.launch {
      val shortcuts = mutableListOf<UIApplicationShortcutItem>()
      shortcuts.add(getScanShortcutItem())
      shortcuts += shortcutList.map {
        getShortItem(it)
      }
      if (shortcuts.count() > maxCount) {
        shortcuts.add(maxCount - 1, getMoreShortcutItem())
      }
      app.shortcutItems = shortcuts
    }
    return true
  }

  /**
   * see: https://github.com/BioforestChain/dweb_browser/issues/62
   */
  private fun getShortItem(item: SystemShortcut): UIApplicationShortcutItem {
    return UIApplicationShortcutItem(
      item.mmid,
      item.title,
      null,
      null,
      mapOf(Pair(item.mmid, item.data))
    )
  }

  private fun getScanShortcutItem(): UIApplicationShortcutItem {
    val title = ShortcutI18nResource.default_qrcode_title.text
    return UIApplicationShortcutItem(
      scanMmid,
      title,
      null,
      UIApplicationShortcutIcon.iconWithSystemImageName("qrcode.viewfinder"),
      null
    )
  }

  private fun getMoreShortcutItem(): UIApplicationShortcutItem {
    val title = ShortcutI18nResource.more_title.text
    return UIApplicationShortcutItem(
      shortcutMmid,
      title,
      null,
      UIApplicationShortcutIcon.iconWithSystemImageName("ellipsis.circle"),
      null
    )
  }

  // iOS: 由于无法正确解析SVG图片，所以iOS会一直返回null.
  actual suspend fun getValidIcon(
    microModule: MicroModule.Runtime,
    resource: ImageResource,
  ): ByteArray? {
    return null
  }
}