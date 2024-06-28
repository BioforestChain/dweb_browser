package org.dweb_browser.sys.device

import org.dweb_browser.helper.SuspendOnce
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

@OptIn(ExperimentalSettingsImplementation::class)
actual object DeviceManage {
  private const val UUID = "ConfigUUID"

  private val keychain = KeychainSettings()
  private val initDeviceUUID = SuspendOnce {
    keychain.getStringOrNull(UUID) ?: UIDevice.currentDevice.identifierForVendor.toString()
      .also { newUUID ->
        debugDevice("deviceUUID", "newUUID=$newUUID")
        keychain.putString(UUID, newUUID)
      }
  }

  actual suspend fun deviceUUID(uuid: String?): String {
    return uuid ?: initDeviceUUID() // 这边因为android改造，特地传入uuid这个值，所以默认情况直接返回uuid
  }

  actual fun deviceAppVersion(): String {
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as String
  }

//  @OptIn(ExperimentalForeignApi::class)
//  actual fun getAboutInfo(): AboutBaseInfo {
//    val arch = memScoped {
//      val systemInfo = alloc<utsname>()
//      if (uname(systemInfo.ptr) == 0) {
//        return@memScoped systemInfo.machine.toKString()
//      } else {
//        "arm64"
//      }
//    }
//
//    return AboutBaseInfo(
//      appName = NSBundle.mainBundle.infoDictionary?.get("CFBundleName") as String?
//        ?: "Dweb Browser",
//      appVersion = deviceAppVersion(),
//      platform = "iOS",
//      arch = arch,
//      modelName = UIDevice.currentDevice.model,
//      osVersion = UIDevice.currentDevice.systemVersion,
//      webviewVersion = NSBundle.bundleWithIdentifier("com.apple.WebKit")
//        ?.objectForInfoDictionaryKey("CFBundleShortVersionString") as String? ?: "Unknown"
//    )
//  }
}