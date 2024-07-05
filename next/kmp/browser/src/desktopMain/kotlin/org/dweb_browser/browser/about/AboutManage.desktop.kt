package org.dweb_browser.browser.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.platform.desktop.webview.jxBrowserEngine
import org.dweb_browser.sys.device.DeviceManage
import org.dweb_browser.sys.device.model.MacHardwareInfoData
import org.dweb_browser.sys.device.model.WinHardwareInfo
import org.dweb_browser.sys.device.model.WinHardwareInfoData

data class DesktopSystemInfo(
  val hostName: String? = null,
  val os: String,
  val osName: String,
  val osVersion: String,
  val arch: String,
  val javaVersion: String,
  val javaVendor: String,
  val totalMemory: Long,
  val freeMemory: Long,
  val maximumMemory: Long,
)

actual suspend fun AboutNMM.AboutRuntime.openAboutPage(id: UUID) {
  val macHardwareInfo = DeviceManage.getMacHardwareInfo()
  val winHardwareInfo = DeviceManage.getWinHardwareInfo()
  val desktopSystemInfo = Runtime.getRuntime().run {
    DesktopSystemInfo(
      hostName = WinHardwareInfo.getHostName(),
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
      totalMemory = totalMemory(),
      freeMemory = freeMemory(),
      maximumMemory = maxMemory(),
    )
  }
  val desktopAppInfo = AboutAppInfo(
    appVersion = DeviceManage.deviceAppVersion(), webviewVersion = jxBrowserEngine.chromiumVersion
  )
  provideAboutRender(id) { modifier ->
    AboutRender(
      modifier = modifier,
      appInfo = desktopAppInfo,
      desktopSystemInfo = desktopSystemInfo,
      macHardwareInfo = macHardwareInfo,
      winHardwareInfo = winHardwareInfo
    )
  }
}

@Composable
fun AboutRender(
  modifier: Modifier,
  appInfo: AboutAppInfo,
  desktopSystemInfo: DesktopSystemInfo,
  macHardwareInfo: MacHardwareInfoData?,
  winHardwareInfo: WinHardwareInfoData?
) {
  LazyColumn(
    modifier = modifier,
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.Top,
  ) {
    item {
      AboutTitle(AboutI18nResource.app())
      AboutAppInfoRender(appInfo)
      AboutHorizontalDivider()
    }
    item("system-info") {
      AboutTitle(AboutI18nResource.system())
      AboutColumnContainer {
        desktopSystemInfo.hostName?.also {
          AboutDetailsItem(labelName = AboutI18nResource.hostName(), text = desktopSystemInfo.hostName)
        }
        AboutDetailsItem(
          labelName = AboutI18nResource.os(), text = desktopSystemInfo.os
        )
        AboutDetailsItem(labelName = AboutI18nResource.osName(), text = desktopSystemInfo.osName)
        AboutDetailsItem(
          labelName = AboutI18nResource.osVersion(), text = desktopSystemInfo.osVersion
        )
        AboutDetailsItem(labelName = AboutI18nResource.arch(), text = desktopSystemInfo.arch)
        AboutDetailsItem(
          labelName = AboutI18nResource.javaVersion(), text = desktopSystemInfo.javaVersion
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.javaVendor(), text = desktopSystemInfo.javaVendor
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.totalMemory(),
          text = desktopSystemInfo.totalMemory.toSpaceSize()
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.freeMemory(),
          text = desktopSystemInfo.freeMemory.toSpaceSize()
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.maximumMemory(),
          text = desktopSystemInfo.maximumMemory.toSpaceSize()
        )
      }
      AboutHorizontalDivider()
    }
    if (macHardwareInfo != null) {
      item("hardware-info") {
        AboutTitle(AboutI18nResource.hardware())
        AboutColumnContainer {
          AboutDetailsItem(
            labelName = AboutI18nResource.modelName(), text = macHardwareInfo.modelName
          )
          AboutDetailsItem(
            labelName = AboutI18nResource.chip(), text = macHardwareInfo.chip
          )
          AboutDetailsItem(
            labelName = "", text = "${macHardwareInfo.ram} ${AboutI18nResource.ram()}"
          )
          macHardwareInfo.cpuCoresNumber?.also {
            AboutDetailsItem(
              labelName ="",
              text = "${macHardwareInfo.cpuCoresNumber!!} CPU ${AboutI18nResource.cpuCoresNumber()}"
            )
          }
          macHardwareInfo.cpuPerformanceCoresNumber?.also {
            AboutDetailsItem(
              labelName = "",
              text = "${macHardwareInfo.cpuPerformanceCoresNumber!!} CPU ${AboutI18nResource.cpuPerformanceCoresNumber()}"
            )
          }
          macHardwareInfo.cpuEfficiencyCoresNumber?.also {
            AboutDetailsItem(
              labelName = "",
              text = "${macHardwareInfo.cpuEfficiencyCoresNumber!!} CPU ${AboutI18nResource.cpuEfficiencyCoresNumber()}"
            )
          }
        }
        AboutHorizontalDivider()
      }
    }

    if (winHardwareInfo != null) {
      item("hardware-info") {
        AboutTitle(AboutI18nResource.hardware())
        AboutColumnContainer {
          winHardwareInfo.cpuInfoList?.also {
            winHardwareInfo.cpuInfoList!!.forEachIndexed { index, cpuInfo ->
              AboutDetailsItem(labelName = "CPU ${index + 1}", text = cpuInfo.name.trim())
              AboutDetailsItem(
                labelName = "",
                text = "${cpuInfo.cpuCoresNumber} CPU ${AboutI18nResource.cpuCoresNumber()}"
              )
              AboutDetailsItem(
                labelName = "",
                text = "${cpuInfo.cpuLogicalProcessorsNumber} CPU ${AboutI18nResource.cpuLogicalProcessorsNumber()}"
              )
            }
          }
          winHardwareInfo.gpuInfoList?.also {
            winHardwareInfo.gpuInfoList!!.forEachIndexed { index, gpuInfo ->
              AboutDetailsItem(
                labelName = "GPU ${index + 1}", text = gpuInfo.name.trim()
              )
              AboutDetailsItem(
                labelName = "", text = "${gpuInfo.ram.toSpaceSize()} ${AboutI18nResource.adapterRam()}"
              )
            }
          }
          winHardwareInfo.memoryInfoList?.also {
            winHardwareInfo.memoryInfoList!!.forEachIndexed { index, memoryInfo ->
              AboutDetailsItem(
                labelName = "RAM ${index + 1} ${AboutI18nResource.manufacturer()}", text = memoryInfo.manufacturer.trim()
              )
              AboutDetailsItem(
                labelName = "", text = "${memoryInfo.capacity.toSpaceSize()} ${AboutI18nResource.capacity()}"
              )
              AboutDetailsItem(
                labelName = "", text = "${memoryInfo.speed}MT/s ${AboutI18nResource.speed()}"
              )
            }
          }
          winHardwareInfo.diskInfoList?.also {
            winHardwareInfo.diskInfoList!!.forEachIndexed { index, diskInfo ->
              AboutDetailsItem(
                labelName = "${AboutI18nResource.disk()} ${index + 1} ${AboutI18nResource.modelName()}", text = diskInfo.model.trim()
              )
              AboutDetailsItem(
                labelName = "", text = "${diskInfo.size.toSpaceSize()} ${AboutI18nResource.capacity()}"
              )
            }
          }
        }
        AboutHorizontalDivider()
      }
    }

    item("env-switch") {
      EnvSwitcherRender()
      AboutHorizontalDivider()
    }
  }
}
