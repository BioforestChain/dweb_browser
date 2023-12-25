package org.dweb_browser.sys.device

import platform.UIKit.UIDevice

actual class DeviceApi actual constructor() {

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

}