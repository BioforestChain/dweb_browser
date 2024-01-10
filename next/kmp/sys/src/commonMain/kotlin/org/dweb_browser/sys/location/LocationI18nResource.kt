package org.dweb_browser.sys.location

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object LocationI18nResource {
  val apply_location_permission =
    SimpleI18nResource(Language.ZH to "申请定位权限", Language.EN to "Apply for your location")

  val request_permission_title = SimpleI18nResource(
    Language.ZH to "定位权限使用说明",
    Language.EN to "Description of locating permission"
  )
  val request_permission_message = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“定位”权限，同意后，将用于为您提供位置信息服务",
    Language.EN to "DwebBrowser is asking you for \"location\" permission, which will be used to provide you with location information services"
  )

  val permission_denied = SimpleI18nResource(
    Language.ZH to "定位权限获取失败，请先进行定位授权，再执行当前操作！",
    Language.EN to "Failed to obtain the locating permission. Please authorize the locating permission before performing the current operation."
  )
}