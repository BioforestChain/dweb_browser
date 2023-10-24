package org.dweb_browser.sys.window

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

class WindowI18nResource {
  companion object {
    val window_dismiss = SimpleI18nResource(
      Language.EN by "Dismiss",
      Language.ZH by "取消",
    )
    val window_confirm = SimpleI18nResource(
      Language.EN by "Confirm",
      Language.ZH by "确认",
    )
    val window_will_be_close = SimpleI18nResource(
      Language.EN by "This Window will be close",
      Language.ZH by "窗口将会关闭",
    )
    val window_confirm_to_close = SimpleI18nResource(
      Language.EN by "Are you sure you want to close this Window?",
      Language.ZH by "确定要关闭窗口？",
    )
  }
}