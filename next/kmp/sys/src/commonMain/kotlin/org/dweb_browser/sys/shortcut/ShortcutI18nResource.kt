package org.dweb_browser.sys.shortcut

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object ShortcutI18nResource {
  val name = SimpleI18nResource(Language.ZH to "快捷指令", Language.EN to "shortcut")
  val shortcut_title = SimpleI18nResource(Language.ZH to "快捷指令", Language.EN to "shortcut")
  val default_qrcode_title = SimpleI18nResource(Language.ZH to "扫一扫", Language.EN to "Scan")
  val more_title = SimpleI18nResource(Language.ZH to "更多", Language.EN to "More")
  val render_no_data =
    SimpleI18nResource(Language.ZH to "没有找到快捷方式", Language.EN to "no found shortcut")
}