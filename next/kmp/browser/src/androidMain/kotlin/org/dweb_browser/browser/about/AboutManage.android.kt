package org.dweb_browser.browser.about

import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.UUID
import org.dweb_browser.sys.device.DeviceManage
import org.dweb_browser.sys.device.model.Battery
import org.dweb_browser.sys.device.model.DeviceData
import org.dweb_browser.sys.device.model.DeviceInfo

data class AndroidSystemInfo(
  val os: String = "Android",
  val osVersion: String,
//  val deviceName: String,
  val sdkInt: Int,
)

actual suspend fun AboutNMM.AboutRuntime.openAboutPage(id: UUID) {
  val deviceData = DeviceInfo.deviceData
  val batteryInfo = DeviceInfo.getBatteryInfo()
  val androidSystemInfo = AndroidSystemInfo(
    osVersion = DeviceInfo.osVersion,
//    deviceName = deviceData.deviceName,
    sdkInt = DeviceInfo.sdkInt
  )
  val appInfo = AboutAppInfo(
    appVersion = DeviceManage.deviceAppVersion(),
    webviewVersion = WebView.getCurrentWebViewPackage()?.versionName ?: "Unknown"
  )
  provideAboutRender(id) { modifier ->
    AboutRender(
      modifier = modifier,
      appInfo = appInfo,
      androidSystemInfo = androidSystemInfo,
      deviceData = deviceData,
      batteryInfo = batteryInfo
    )
  }
}

@Composable
fun AboutRender(
  modifier: Modifier,
  appInfo: AboutAppInfo,
  androidSystemInfo: AndroidSystemInfo,
  deviceData: DeviceData,
  batteryInfo: Battery,
) {
  LazyColumn(
    modifier = modifier,
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.Top,
  ) {
    item("app-info") {
      AboutTitle(AboutI18nResource.app())
      AboutAppInfoRender(appInfo)
      AboutHorizontalDivider()
    }
    item("system-info") {
      AboutTitle(AboutI18nResource.system())
      AboutContainer {
        AboutDetailsItem(
          labelName = AboutI18nResource.os(), text = androidSystemInfo.os
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.osVersion(), text = androidSystemInfo.osVersion
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.sdkInt(), text = androidSystemInfo.sdkInt.toString()
        )
//        AboutDetailsItem(
//          labelName = AboutI18nResource.deviceName(), text = androidSystemInfo.deviceName
//        )
      }
      AboutHorizontalDivider()
    }
    item("hardware-info") {
      AboutTitle(AboutI18nResource.hardware())
      AboutContainer {
        AboutDetailsItem(
          labelName = AboutI18nResource.brand(), text = deviceData.brand
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.modelName(), text = deviceData.deviceModel
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.hardware(), text = deviceData.hardware
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.supportAbis(), text = deviceData.supportAbis
        )
        AboutDetailsItem(
          labelName = "ID", text = deviceData.id
        )
        AboutDetailsItem(
          labelName = "DISPLAY", text = deviceData.display
        )
        AboutDetailsItem(
          labelName = "BOARD", text = deviceData.board
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.manufacturer(), text = deviceData.manufacturer
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.display(), text = deviceData.screenSizeInches
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.resolution(), text = deviceData.resolution
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.density(), text = deviceData.density
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.refreshRate(), text = deviceData.refreshRate
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.memory(),
          text = "${deviceData.memory!!.usage}/${deviceData.memory!!.total}"
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.storage(),
          text = "${deviceData.storage!!.internalUsageSize}/${deviceData.storage!!.internalTotalSize}"
        )
      }
      AboutHorizontalDivider()
    }
    item("battery-info") {
      AboutTitle(AboutI18nResource.battery())
      AboutContainer {
        AboutDetailsItem(
          labelName = AboutI18nResource.status(),
          text = if (batteryInfo.isPhoneCharging) AboutI18nResource.charging() else AboutI18nResource.discharging()
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.health(), text = batteryInfo.batteryHealth ?: "Unknown"
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.percent(), text = "${batteryInfo.batteryPercent}%"
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
