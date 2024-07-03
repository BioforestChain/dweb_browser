package org.dweb_browser.sys.device.model

import com.sun.jna.Native
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.execCommand
import org.dweb_browser.helper.toSpaceSize

interface Kernel32 : StdCallLibrary {
  fun GetComputerNameEx(
    nameType: Int,
    lpBuffer: CharArray,
    nSize: IntByReference
  ): Boolean

  companion object {
    const val ComputerNamePhysicalDnsHostname = 5
    val INSTANCE: Kernel32 =
      Native.load("kernel32", Kernel32::class.java, W32APIOptions.DEFAULT_OPTIONS)
  }
}

data class WinHardwareInfoData(
  val hostName: String? = null,
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

  private fun getHostName(): String? {
    val nSize = IntByReference(1024)
    val buffer = CharArray(1024)
    if (Kernel32.INSTANCE.GetComputerNameEx(
        Kernel32.ComputerNamePhysicalDnsHostname,
        buffer,
        nSize
      )
    ) {
      return String(buffer, 0, nSize.value)
    } else {
      return null
    }
  }

  fun getHardwareInfo(): WinHardwareInfoData? {
    if (!PureViewController.isWindows) {
      return null
    }

    return WinHardwareInfoData(
      hostName = getHostName(),
      ram = ram,
      chip = chip,
      cpuCoresNumber = cpuCoresNumber,
      cpuLogicalProcessorsNumber = cpuLogicalProcessorsNumber
    )
  }
}