package org.dweb_browser.sys.permission

/**
 * 权限的列表，用于不同平台根据类型进行权限获取
 * 随着sys的完善，这里会逐渐补全
 */
enum class SystemPermissionName {
  CALENDAR,
  CAMERA,
  CONTACTS,
  LOCATION,
  MICROPHONE,
  PHONE,
  SENSORS,
  SMS,
  STORAGE,
  CALL,
  CLIPBOARD,
  ;
}