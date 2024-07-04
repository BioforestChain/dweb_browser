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
import org.dweb_browser.sys.device.model.WinHardwareInfoData

data class DesktopSystemInfo(
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
      AboutContainer {
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
        AboutContainer {
          AboutDetailsItem(
            labelName = AboutI18nResource.modelName(), text = macHardwareInfo.modelName
          )
          AboutDetailsItem(
            labelName = AboutI18nResource.chip(), text = macHardwareInfo.chip
          )
          AboutDetailsItem(
            labelName = AboutI18nResource.ram(), text = macHardwareInfo.ram
          )
          macHardwareInfo.cpuCoresNumber?.also {
            AboutDetailsItem(
              labelName = AboutI18nResource.cpuCoresNumber(),
              text = macHardwareInfo.cpuCoresNumber!!
            )
          }
          macHardwareInfo.cpuPerformanceCoresNumber?.also {
            AboutDetailsItem(
              labelName = AboutI18nResource.cpuPerformanceCoresNumber(),
              text = macHardwareInfo.cpuPerformanceCoresNumber!!
            )
          }
          macHardwareInfo.cpuEfficiencyCoresNumber?.also {
            AboutDetailsItem(
              labelName = AboutI18nResource.cpuEfficiencyCoresNumber(),
              text = macHardwareInfo.cpuEfficiencyCoresNumber!!
            )
          }
        }
        AboutHorizontalDivider()
      }
    }

    if (winHardwareInfo != null) {
      item("hardware-info") {
        AboutTitle(AboutI18nResource.hardware())
        AboutContainer {
          winHardwareInfo.hostName?.also {
            AboutDetailsItem(
              labelName = AboutI18nResource.hostName(), text = winHardwareInfo.hostName!!
            )
          }
          winHardwareInfo.cpuInfoList?.also {
            winHardwareInfo.cpuInfoList!!.forEachIndexed { index, cpuInfo ->
              AboutDetailsItem(labelName = "CPU ${index + 1}", text = "")
              AboutDetailsItem(labelName = AboutI18nResource.chip(), text = cpuInfo.name.trim())
              AboutDetailsItem(
                labelName = AboutI18nResource.cpuCoresNumber(),
                text = cpuInfo.cpuCoresNumber.toString()
              )
              AboutDetailsItem(
                labelName = AboutI18nResource.cpuLogicalProcessorsNumber(),
                text = cpuInfo.cpuLogicalProcessorsNumber.toString()
              )
            }
          }
          winHardwareInfo.gpuInfoList?.also {
            winHardwareInfo.gpuInfoList!!.forEachIndexed { index, gpuInfo ->
              AboutDetailsItem(labelName = "GPU ${index + 1}", text = "")
              AboutDetailsItem(
                labelName = AboutI18nResource.videoController(), text = gpuInfo.name.trim()
              )
              AboutDetailsItem(
                labelName = AboutI18nResource.adapterRam(), text = gpuInfo.ram.toSpaceSize()
              )
            }
          }
          winHardwareInfo.memoryInfoList?.also {
            winHardwareInfo.memoryInfoList!!.forEachIndexed { index, memoryInfo ->
              AboutDetailsItem(labelName = "RAM ${index + 1}", text = "")
              AboutDetailsItem(
                labelName = AboutI18nResource.manufacturer(), text = memoryInfo.manufacturer.trim()
              )
              AboutDetailsItem(
                labelName = AboutI18nResource.capacity(), text = memoryInfo.capacity.toSpaceSize()
              )
              AboutDetailsItem(
                labelName = AboutI18nResource.speed(), text = "${memoryInfo.speed}MT/s"
              )
            }
          }
          winHardwareInfo.diskInfoList?.also {
            winHardwareInfo.diskInfoList!!.forEachIndexed { index, diskInfo ->
              AboutDetailsItem(labelName = "${AboutI18nResource.disk} ${index + 1}", text = "")
              AboutDetailsItem(
                labelName = AboutI18nResource.modelName(), text = diskInfo.model.trim()
              )
              AboutDetailsItem(
                labelName = AboutI18nResource.capacity(), text = diskInfo.size.toSpaceSize()
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
