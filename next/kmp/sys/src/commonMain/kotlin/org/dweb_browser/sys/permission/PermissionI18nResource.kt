package org.dweb_browser.sys.permission

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object PermissionI18nResource {
  val name = SimpleI18nResource(Language.ZH to "权限管理", Language.EN to "Permission Management")
  val short_name = SimpleI18nResource(Language.ZH to "权限", Language.EN to "Permission")

  val no_record =
    SimpleI18nResource(Language.ZH to "暂无授权记录", Language.EN to "No Authorization Record")
  val record_list_title =
    SimpleI18nResource(Language.ZH to "授权记录", Language.EN to "Authorization Records")

  val record_state_unknown = SimpleI18nResource(Language.ZH to "询问", Language.EN to "Ask")
  val record_state_granted = SimpleI18nResource(Language.ZH to "允许", Language.EN to "Allow")
  val record_state_denied = SimpleI18nResource(Language.ZH to "禁止", Language.EN to "Forbid")
  val remove_record = SimpleI18nResource(Language.ZH to "移除记录", Language.EN to "Remove record")

  val request_title =
    SimpleI18nResource(Language.ZH to "申请权限", Language.EN to "Request Permission")

  val request_button_refuse = SimpleI18nResource(Language.ZH to "拒绝", Language.EN to "Refuse")
  val request_button_confirm = SimpleI18nResource(Language.ZH to "确定", Language.EN to "Confirm")
  val request_button_authorize_all =
    SimpleI18nResource(Language.ZH to "授权全部", Language.EN to "Authorize All")
}