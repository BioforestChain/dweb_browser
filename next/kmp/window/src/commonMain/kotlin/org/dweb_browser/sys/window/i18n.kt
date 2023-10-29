package org.dweb_browser.sys.window

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

class WindowI18nResource {
  companion object {
    val window_dismiss = SimpleI18nResource(
      Language.EN to "Dismiss",
      Language.ZH to "取消",
    )
    val window_confirm = SimpleI18nResource(
      Language.EN to "Confirm",
      Language.ZH to "确认",
    )
    val window_will_be_close = SimpleI18nResource(
      Language.EN to "This Window will be close",
      Language.ZH to "窗口将会关闭",
    )
    val application_will_be_close = SimpleI18nResource(
      Language.EN to "This Application will be close",
      Language.ZH to "应用将会关闭",
    )
    val window_confirm_to_close = SimpleI18nResource(
      Language.EN to "Are you sure you want to close this Window?",
      Language.ZH to "确定要关闭窗口？",
    )
  }
}