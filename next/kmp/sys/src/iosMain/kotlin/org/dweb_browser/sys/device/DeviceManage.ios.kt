package org.dweb_browser.sys.device

import org.dweb_browser.helper.WARNING
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

@OptIn(ExperimentalSettingsImplementation::class)
actual object DeviceManage {

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
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as String
  }
}