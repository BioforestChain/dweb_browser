package org.dweb_browser.sys.device.model

import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.execCommand
import org.dweb_browser.helper.toSpaceSize

data class WinHardwareInfoData(
  val ram: String? = null,
  var chip: String? = null,
  var cpuCoresNumber: String? = null,
  var cpuLogicalProcessorsNumber: String? = null
)

object WinHardwareInfo {
  val chip by lazy {
    try {
      execCommand("Get-WmiObject -Class Win32_Processor | Select-Object -ExpandProperty Name")
    } catch (_: Exception) {
      null
    }
  }

  val cpuCoresNumber by lazy {
    try {
      execCommand("Get-WmiObject -Class Win32_Processor | Select-Object -ExpandProperty NumberOfCores")
    } catch (_: Exception) {
      null
    }
  }

  val cpuLogicalProcessorsNumber by lazy {
    try {
      execCommand("Get-WmiObject -Class Win32_Processor | Select-Object -ExpandProperty NumberOfLogicalProcessors")
    } catch (_: Exception) {
      null
    }
  }

  val ram by lazy {
    try {
      execCommand("Get-WmiObject -Class Win32_PhysicalMemory | Select-Object -ExpandProperty Capacity").let { ramString ->
        (ramString.split("\\s+".toRegex())
          .fold(0L) { totalRam, item -> item.toLong() + totalRam }).toSpaceSize()
      }
    } catch (_: Exception) {
      null
    }
  }

  fun getHardwareInfo(): WinHardwareInfoData? {
    if (!PureViewController.isWindows) {
      return null
    }

    return WinHardwareInfoData(
      ram = ram,
      chip = chip,
      cpuCoresNumber = cpuCoresNumber,
      cpuLogicalProcessorsNumber = cpuLogicalProcessorsNumber
    )
  }
}