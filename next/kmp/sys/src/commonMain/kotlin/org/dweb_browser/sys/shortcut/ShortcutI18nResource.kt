package org.dweb_browser.sys.shortcut

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object ShortcutI18nResource {
  val default_qrcode_title = SimpleI18nResource(Language.ZH to "扫一扫", Language.EN to "Scan")
  val render_no_data =
    SimpleI18nResource(Language.ZH to "没有找到快捷方式", Language.EN to "no found shortcut")
}