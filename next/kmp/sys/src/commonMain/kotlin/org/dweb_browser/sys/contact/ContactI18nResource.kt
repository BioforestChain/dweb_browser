package org.dweb_browser.sys.contact

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object ContactI18nResource {
  val request_permission_title_contact = SimpleI18nResource(
    Language.ZH to "相机权限使用说明",
    Language.EN to "Camera permission instructions"
  )
  val request_permission_message_pick_contact = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“联系人”权限，同意后，将用于为您提供联系人信息服务",
    Language.EN to "DwebBrowser is asking you for \"Contact\" permissions, and if you agree, it will be used to provide you with contact information services"
  )

  val permission_denied_pick_contact = SimpleI18nResource(
    Language.ZH to "权限被拒绝，无法提供联系人信息服务",
    Language.EN to "The permission is denied, and the photo service cannot be provided"
  )

  val data_is_null = SimpleI18nResource(
    Language.ZH to "数据为空",
    Language.EN to "The data is null"
  )
}