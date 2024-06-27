package org.dweb_browser.sys.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.platform.desktop.webview.WebviewEngine
import org.dweb_browser.sys.device.DesktopHardwareInfo
import org.dweb_browser.sys.device.DeviceManage
import org.dweb_browser.sys.window.core.windowAdapterManager

data class DesktopSystemInfo(
  val os: String,
  val osName: String,
  val osVersion: String,
  val arch: String,
  val cpuCoresNumber: Int,
  val javaVersion: String,
  val javaVendor: String,
  val totalMemory: Long,
  val freeMemory: Long,
  val maximumMemory: Long,
)

actual suspend fun AboutNMM.AboutRuntime.openAboutPage(id: UUID) {
  val desktopHardwareInfo = DeviceManage.getHardwareInfo()
  val desktopSystemInfo = Runtime.getRuntime().run {
    DesktopSystemInfo(
      os = when {
        PureViewController.isMacOS -> "MacOS"
        PureViewController.isWindows -> "Windows"
        else -> "Unknown"
      },
      osName = System.getProperty("os.name"),
      osVersion = System.getProperty("os.version"),
      arch = System.getProperty("os.arch"),
      javaVersion = System.getProperty("java.version"),
      javaVendor = System.getProperty("java.vendor"),
      cpuCoresNumber = availableProcessors(),
      totalMemory = totalMemory(),
      freeMemory = freeMemory(),
      maximumMemory = maxMemory(),
    )
  }
  val desktopAppInfo = AboutAppInfo(
    appVersion = DeviceManage.deviceAppVersion(), webviewVersion = WebviewEngine.chromiumVersion
  )
  windowAdapterManager.provideRender(id) { modifier ->
    AboutRender(
      modifier = modifier,
      appInfo = desktopAppInfo,
      desktopSystemInfo = desktopSystemInfo,
      desktopHardwareInfo = desktopHardwareInfo
    )
  }
}

@Composable
fun AboutRender(
  modifier: Modifier,
  appInfo: AboutAppInfo,
  desktopHardwareInfo: DesktopHardwareInfo,
  desktopSystemInfo: DesktopSystemInfo
) {
  Box(modifier = modifier.fillMaxSize().background(Color.Gray)) {
    Column(
      modifier = Modifier.verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.Start,
      verticalArrangement = Arrangement.Top
    ) {
      Text(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        text = "应用",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      AboutAppInfoRender(appInfo)
      HorizontalDivider()
      Text(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        text = "系统",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().background(
          color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)
        )
      ) {
        AboutDetailsItem(
          labelName = AboutI18nResource.os.text, text = desktopSystemInfo.os
        )
        AboutDetailsItem(labelName = AboutI18nResource.osName.text, text = desktopSystemInfo.osName)
        AboutDetailsItem(
          labelName = AboutI18nResource.osVersion.text, text = desktopSystemInfo.osVersion
        )
        AboutDetailsItem(labelName = AboutI18nResource.arch.text, text = desktopSystemInfo.arch)
        AboutDetailsItem(
          labelName = AboutI18nResource.cpuCoresNumber.text,
          text = desktopSystemInfo.cpuCoresNumber.toString()
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.javaVersion.text, text = desktopSystemInfo.javaVersion
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.javaVendor.text, text = desktopSystemInfo.javaVendor
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.totalMemory.text,
          text = desktopSystemInfo.totalMemory.toString()
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.freeMemory.text,
          text = desktopSystemInfo.freeMemory.toString()
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.maximumMemory.text,
          text = desktopSystemInfo.maximumMemory.toString()
        )
      }
      HorizontalDivider()
      Text(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        text = "硬件",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().background(
          color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)
        )
      ) {
        AboutDetailsItem(
          labelName = AboutI18nResource.modelName.text, text = desktopHardwareInfo.modelName
        )
      }
    }
  }
}