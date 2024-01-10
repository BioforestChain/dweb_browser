package org.dweb_browser.sys.microphone

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object MicroPhoneI18nResource {
  val request_permission_title = SimpleI18nResource(
    Language.ZH to "麦克风权限使用说明",
    Language.EN to "MicroPhone permission instructions"
  )
  val request_permission_message = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“麦克风”权限，同意后，将用于为您提供拍照服务",
    Language.EN to "DwebBrowser is asking you for \"Camera\" permissions, and if you agree, it will be used to provide you with photo services"
  )
  val permission_denied = SimpleI18nResource(
    Language.ZH to "权限被拒绝，无法提供录音服务",
    Language.EN to "The permission is denied,  and recording services cannot be provided. Procedure"
  )
  val data_is_null = SimpleI18nResource(
    Language.ZH to "数据为空",
    Language.EN to "The data is null"
  )
}