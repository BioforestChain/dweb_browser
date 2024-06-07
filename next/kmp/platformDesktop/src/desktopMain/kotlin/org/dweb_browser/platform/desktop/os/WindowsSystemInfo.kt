package org.dweb_browser.platform.desktop.os

object WindowsSystemInfo {
  val osName: String = System.getProperty("os.name")
  val osVersion: String = System.getProperty("os.version")

  val isWin10 = osName == "Windows 10"
  val isWin11 = osName == "Windows 11"

  // win10 和 win11 的 os.version 都是 10.0
//  val isWindows10OrGreater = osVersion.toFloat() >= 10 java.lang.NumberFormatException: multiple points
}