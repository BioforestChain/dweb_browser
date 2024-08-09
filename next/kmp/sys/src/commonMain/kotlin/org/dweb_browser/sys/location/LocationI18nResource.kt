package org.dweb_browser.sys.location

import org.dweb_browser.helper.compose.I18n

object LocationI18nResource : I18n() {
  val name = zh("地理位置", "Geolocation")
  val apply_location_permission = zh("申请定位权限", "Apply for your location")
  
  val request_permission_title = zh("定位权限使用说明", "Description of locating permission")
  val request_permission_message = zh(
    "DwebBrowser正在向您获取“定位”权限，同意后，将用于为您提供位置信息服务",
    "DwebBrowser is asking you for \"location\" permission, which will be used to provide you with location information services"
  )
  
  val permission_denied = zh(
    "定位权限获取失败，请先进行定位授权，再执行当前操作！",
    "Failed to obtain the locating permission. Please authorize the locating permission before performing the current operation."
  )
}