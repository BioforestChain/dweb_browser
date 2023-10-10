package org.dweb_browser.sys.window

import org.dweb_browser.helper.compose.Lang
import org.dweb_browser.helper.compose.SimpleI18nResource

class WindowI18nResource {
  companion object {
    val window_dismiss = SimpleI18nResource(
      Lang.EN by "Dismiss",
      Lang.ZH_CN by "取消",
    )
    val window_confirm = SimpleI18nResource(
      Lang.EN by "Confirm",
      Lang.ZH_CN by "确认",
    )
    val window_will_be_close = SimpleI18nResource(
      Lang.EN by "This Window will be close",
      Lang.ZH_CN by "窗口将会关闭",
    )
    val window_confirm_to_close = SimpleI18nResource(
      Lang.EN by "Are you sure you want to close this Window?",
      Lang.ZH_CN by "确定要关闭窗口？",
    )
  }
}