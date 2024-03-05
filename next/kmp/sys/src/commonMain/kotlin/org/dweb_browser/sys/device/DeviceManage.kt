package org.dweb_browser.sys.device

expect object DeviceManage {

  fun deviceUUID(): String

  fun deviceAppVersion(): String
}