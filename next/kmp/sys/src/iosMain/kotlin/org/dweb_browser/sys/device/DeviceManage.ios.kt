package org.dweb_browser.sys.device

import org.dweb_browser.helper.WARNING
import platform.UIKit.UIDevice

@OptIn(ExperimentalSettingsImplementation::class)
actual class DeviceManage actual constructor() {

  private val configUUID = "ConfigUUID"

  private val keychain = KeychainSettings()
  actual fun deviceUUID(): String {

    var uuid = keychain.getStringOrNull(configUUID)
    if (uuid == null) {
      uuid = UIDevice.currentDevice.identifierForVendor.toString()
      keychain.putString(configUUID, uuid)
    }
    return uuid
  }

  actual fun deviceAppVersion(): String {
    WARNING("Not yet implemented deviceAppVersion")
    return ""
  }
}