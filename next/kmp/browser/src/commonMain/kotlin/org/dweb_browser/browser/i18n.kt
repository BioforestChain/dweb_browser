package org.dweb_browser.browser

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource


class BrowserI18nResource {
  companion object {
    val dialog_title_webview_upgrade = SimpleI18nResource(Language.ZH by "更新提示")
    val dialog_text_webview_upgrade =
      SimpleI18nResource(Language.ZH by "由于当前系统 Android System Webview 版本过低，无法进行安装。\n请将系统 Android System Webview 版本更新至（%s）后再重试。")
    val dialog_confirm_webview_upgrade = SimpleI18nResource(Language.ZH by "确定")
    val dialog_dismiss_webview_upgrade = SimpleI18nResource(Language.ZH by "帮助文档")
  }
}