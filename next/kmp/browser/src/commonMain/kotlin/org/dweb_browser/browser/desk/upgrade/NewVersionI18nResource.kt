package org.dweb_browser.browser.desk.upgrade

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object NewVersionI18nResource {
  val toast_message_download_fail =
    SimpleI18nResource(Language.ZH to "下载失败", Language.EN to "Download Fail")

  val toast_message_storage_fail = SimpleI18nResource(
    Language.ZH to "文件存储失败，请重新下载",
    Language.EN to "File storage failed, please download again"
  )

  val toast_message_permission_fail =
    SimpleI18nResource(Language.ZH to "授权失败", Language.EN to "authorization failed")

  val request_permission_title_install = SimpleI18nResource(
    Language.ZH to "请求安装应用权限",
    Language.EN to "Request permission to install the application"
  )
  val request_permission_message_install = SimpleI18nResource(
    Language.ZH to "安装应用需要请求安装应用权限，请手动设置",
    Language.EN to "To install an application, you need to request the permission to install the application"
  )

  val request_permission_title_storage = SimpleI18nResource(
    Language.ZH to "请求外部存储权限", Language.EN to "Request external storage permissions"
  )
  val request_permission_message_storage = SimpleI18nResource(
    Language.ZH to "DwebBrowser正在向您获取“存储”权限，同意后，将用于存储下载的应用",
    Language.EN to "DwebBrowser is asking you to \"store\" permission, if you agree, it will be used to store the downloaded application"
  )
}