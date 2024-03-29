package org.dweb_browser.sys.permission

import kotlinx.serialization.Serializable

/**
 * 权限的列表，用于不同平台根据类型进行权限获取
 * 随着sys的完善，这里会逐渐补全
 */
@Serializable
enum class SystemPermissionName {
  CALENDAR,
  CAMERA,
  CONTACTS,
  LOCATION,
  MICROPHONE, // 对应 Manifest.permission.RECORD_AUDIO
  PHONE,
  SENSORS,
  SMS,
  STORAGE,
  CALL,
  CLIPBOARD,
  FILE_CHOOSER,
  Notification, // 用于获取通知的权限
  ;
}