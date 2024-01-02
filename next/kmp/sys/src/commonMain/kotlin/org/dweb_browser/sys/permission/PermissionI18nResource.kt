package org.dweb_browser.sys.permission

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object PermissionI18nResource {
  val no_record =
    SimpleI18nResource(Language.ZH to "暂无授权记录", Language.EN to "No Authorization Record")
  val record_list_title =
    SimpleI18nResource(Language.ZH to "授权记录", Language.EN to "Authorization Records")

  val record_state_unknown = SimpleI18nResource(Language.ZH to "询问", Language.EN to "Ask")
  val record_state_granted = SimpleI18nResource(Language.ZH to "允许", Language.EN to "Allow")
  val record_state_denied = SimpleI18nResource(Language.ZH to "禁止", Language.EN to "Forbid")
  val remove_record = SimpleI18nResource(Language.ZH to "移除记录", Language.EN to "Remove record")
}