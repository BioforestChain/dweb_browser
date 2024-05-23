package org.dweb_browser.sys.device

import kotlinx.coroutines.runBlocking
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

@OptIn(ExperimentalSettingsImplementation::class)
actual object DeviceManage {
  private const val UUID = "ConfigUUID"

  private val keychain = KeychainSettings()
  private val deviceUUID by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    runBlocking {
      keychain.getStringOrNull(UUID) ?: UIDevice.currentDevice.identifierForVendor.toString()
        .also { newUUID ->
          debugDevice("deviceUUID", "newUUID=$newUUID")
          keychain.putString(UUID, newUUID)
        }
    }
  }

  actual fun deviceUUID(uuid: String?): String {
    return uuid ?: deviceUUID // 这边因为android改造，特地传入uuid这个值，所以默认情况直接返回uuid
  }

  actual fun deviceAppVersion(): String {
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as String
  }
}