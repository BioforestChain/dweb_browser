package org.dweb_browser.browser.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.compose.hex
import org.dweb_browser.sys.device.DeviceManage
import org.dweb_browser.sys.device.model.BatteryInfo
import org.dweb_browser.sys.device.model.DeviceInfo
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.render.LocalWindowControllerTheme
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice
import platform.posix.uname
import platform.posix.utsname

data class IOSSystemInfo(
  val os: String,
  val osName: String,
  val osVersion: String,
  val arch: String,
)

data class IOSHardwareInfo(
  val deviceName: String,
  val modelName: String,
)

@OptIn(ExperimentalForeignApi::class)
actual suspend fun AboutNMM.AboutRuntime.openAboutPage(id: UUID) {
  val appInfo = AboutAppInfo(
    appVersion = DeviceManage.deviceAppVersion(),
    webviewVersion = NSBundle.bundleWithIdentifier("com.apple.WebKit")
      ?.objectForInfoDictionaryKey("CFBundleShortVersionString") as String? ?: "Unknown"
  )
  val iosSystemInfo = IOSSystemInfo(
    os = "iOS",
    osName = UIDevice.currentDevice.systemName,
    osVersion = UIDevice.currentDevice.systemVersion,
    arch = memScoped {
      val systemInfo = alloc<utsname>()
      if (uname(systemInfo.ptr) == 0) {
        return@memScoped systemInfo.machine.toKString()
      } else {
        "arm64"
      }
    }
  )
  val iosHardwareInfo = IOSHardwareInfo(
    deviceName = DeviceInfo.deviceName,
    modelName = DeviceInfo.modelName,
  )
  windowAdapterManager.provideRender(id) { modifier ->
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
  batteryInfo: BatteryInfo
) {
  Box(
    modifier = modifier.fillMaxSize()
      .background(
        if (LocalWindowControllerTheme.current.isDark) Color.Black else (Color.hex("#F5F5FA")
          ?: Color.Gray)
      )
  ) {
    Column(
      modifier = Modifier.verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.Start,
      verticalArrangement = Arrangement.Top
    ) {
      Text(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        text = AboutI18nResource.app.text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      AboutAppInfoRender(appInfo)
      Spacer(Modifier.padding(8.dp))
      Text(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        text = AboutI18nResource.system.text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().background(
          color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)
        )
      ) {
        AboutDetailsItem(
          labelName = AboutI18nResource.os.text, text = iosSystemInfo.os
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.osName.text, text = iosSystemInfo.osName
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.osVersion.text, text = iosSystemInfo.osVersion
        )
        AboutDetailsItem(labelName = AboutI18nResource.arch.text, text = iosSystemInfo.arch)
      }
      Spacer(Modifier.padding(8.dp))
      Text(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        text = AboutI18nResource.hardware.text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().background(
          color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)
        )
      ) {
        AboutDetailsItem(
          labelName = AboutI18nResource.deviceName.text, text = iosHardwareInfo.deviceName
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.modelName.text, text = iosHardwareInfo.modelName
        )
      }
      Spacer(Modifier.padding(8.dp))
      Text(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        text = AboutI18nResource.battery.text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().background(
          color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)
        )
      ) {
        AboutDetailsItem(
          labelName = AboutI18nResource.percent.text, text = batteryInfo.batteryLevel.toString()
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.status.text,
          text = if (batteryInfo.isCharging) AboutI18nResource.charging.text else AboutI18nResource.discharging.text
        )
      }
    }
  }
}