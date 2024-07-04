package org.dweb_browser.browser.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.UUID
import org.dweb_browser.sys.device.DeviceManage
import org.dweb_browser.sys.device.model.BatteryInfo
import org.dweb_browser.sys.device.model.DeviceInfo
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

data class IOSSystemInfo(
  val os: String,
  val osName: String,
  val osVersion: String,
  val arch: String,
)

data class IOSHardwareInfo(
  val deviceName: String,
  val modelName: String,
  val brightness: String,
  val ram: String,
  val gpu: String,
  val cpuCoresNumber: Int,
  val hasDynamicIsLand: Boolean
)

actual suspend fun AboutNMM.AboutRuntime.openAboutPage(id: UUID) {
  val appInfo = AboutAppInfo(
    appVersion = DeviceManage.deviceAppVersion(),
    webviewVersion = NSBundle.bundleWithIdentifier("com.apple.WebKit")
      ?.objectForInfoDictionaryKey("CFBundleVersion") as String? ?: "Unknown"
  )
  val iosSystemInfo = IOSSystemInfo(
    os = "iOS",
    osName = UIDevice.currentDevice.systemName,
    osVersion = UIDevice.currentDevice.systemVersion,
    arch = "aarch64"
  )
  val iosHardwareInfo = IOSHardwareInfo(
    deviceName = DeviceInfo.deviceName,
    modelName = DeviceInfo.modelName,
    brightness = DeviceInfo.brightness,
    ram = DeviceInfo.ram,
    gpu = DeviceInfo.gpu,
    cpuCoresNumber = DeviceInfo.cpuCoresNumber,
    hasDynamicIsLand = DeviceInfo.hasDynamicIsland
  )
  provideAboutRender(id) { modifier ->
    AboutRender(
      modifier = modifier,
      appInfo = appInfo,
      iosSystemInfo = iosSystemInfo,
      iosHardwareInfo = iosHardwareInfo,
      batteryInfo = DeviceInfo.batteryInfo
    )
  }
}


@Composable
fun AboutRender(
  modifier: Modifier,
  appInfo: AboutAppInfo,
  iosHardwareInfo: IOSHardwareInfo,
  iosSystemInfo: IOSSystemInfo,
  batteryInfo: BatteryInfo,
) {
  LazyColumn(
    modifier = modifier,
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.Top
  ) {
    item("app-info") {
      AboutTitle(AboutI18nResource.app())
      AboutAppInfoRender(appInfo)
      AboutHorizontalDivider()
    }
    item("system-info") {
      AboutTitle(AboutI18nResource.system())
      AboutColumnContainer {
        AboutDetailsItem(
          labelName = AboutI18nResource.os(), text = iosSystemInfo.os
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.osName(), text = iosSystemInfo.osName
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.osVersion(), text = iosSystemInfo.osVersion
        )
        AboutDetailsItem(labelName = AboutI18nResource.arch(), text = iosSystemInfo.arch)
      }
      AboutHorizontalDivider()
      AboutTitle(AboutI18nResource.hardware())
      AboutColumnContainer {
        AboutDetailsItem(
          labelName = AboutI18nResource.deviceName(), text = iosHardwareInfo.deviceName
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.modelName(), text = iosHardwareInfo.modelName
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.brightness(), text = iosHardwareInfo.brightness
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.ram(), text = iosHardwareInfo.ram
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.cpuCoresNumber(),
          text = iosHardwareInfo.cpuCoresNumber.toString()
        )
        AboutDetailsItem(
          labelName = "GPU", text = iosHardwareInfo.gpu
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.dynamicIsland(),
          text = if (iosHardwareInfo.hasDynamicIsLand) AboutI18nResource.isTrue() else AboutI18nResource.isFalse()
        )
      }
      AboutHorizontalDivider()
    }
    item("battery-info") {
      AboutTitle(AboutI18nResource.battery())
      AboutColumnContainer {
        AboutDetailsItem(
          labelName = AboutI18nResource.percent(), text = batteryInfo.electricity
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.status(),
          text = if (batteryInfo.isCharging) AboutI18nResource.charging() else AboutI18nResource.discharging()
        )
      }
      AboutHorizontalDivider()
    }
    item("env-switch") {
      EnvSwitcherRender()
      AboutHorizontalDivider()
    }
  }
}
