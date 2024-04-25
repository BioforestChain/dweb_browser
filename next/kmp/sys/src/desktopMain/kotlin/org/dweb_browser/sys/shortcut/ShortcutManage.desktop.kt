package org.dweb_browser.sys.shortcut

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.WARNING

actual class ShortcutManage actual constructor() {
  /**
   * 一些默认的系统快捷入口配置
   */
  actual suspend fun initShortcut(microModule: MicroModule.Runtime) {
  }

  /**
   * 动态注册的快捷列表
   */
  actual suspend fun registryShortcut(shortcutList: List<SystemShortcut>): Boolean {
    // TODO Not yet implemented registryShortcut
    WARNING("Not yet implemented registryShortcut")
    return false
  }

  /**
   * 用于进一步的配置shortcut icon.
   * iOS: 由于无法正确解析SVG图片，所以iOS会一直返回null.
   * Android: 如果icon=null, 会使用resource的生成新的icon.
   */
  actual suspend fun getValidIcon(
    microModule: MicroModule.Runtime,
    resource: ImageResource,
  ): ByteArray? {
    // TODO Not yet implemented getValidIcon
    WARNING("Not yet implemented getValidIcon")
    return null
  }

}