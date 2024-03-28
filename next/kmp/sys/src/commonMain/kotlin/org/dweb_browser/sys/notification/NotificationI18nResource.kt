package org.dweb_browser.sys.notification

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object NotificationI18nResource {
  val apply_notification_permission =
    SimpleI18nResource(Language.ZH to "申请通知权限", Language.EN to "Request permission to notify")

  val request_permission_title = SimpleI18nResource(
    Language.ZH to "通知权限使用说明",
    Language.EN to "Description of notification permission"
  )
  val request_permission_message = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“通知”权限，同意后，将用于为您提供通知提醒服务",
    Language.EN to "DwebBrowser is asking you for \"location\" permission, which will be used to provide you with a notification reminder service"
  )

  val permission_denied = SimpleI18nResource(
    Language.ZH to "通知权限获取失败，请先进行授权，再执行当前操作！",
    Language.EN to "The notification permission fails to be obtained. Authorize the notification permission before performing the current operation."
  )
}