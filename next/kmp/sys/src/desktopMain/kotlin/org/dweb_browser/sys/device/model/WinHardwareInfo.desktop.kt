package org.dweb_browser.sys.device.model

import com.sun.jna.Native
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.hardware_info.getHardwareInfo as getWinHardwareInfo

interface Kernel32 : StdCallLibrary {
  fun GetComputerNameEx(
    nameType: Int, lpBuffer: CharArray, nSize: IntByReference
  ): Boolean

  companion object {
    const val ComputerNamePhysicalDnsHostname = 5
    val INSTANCE: Kernel32 =
      Native.load("kernel32", Kernel32::class.java, W32APIOptions.DEFAULT_OPTIONS)
  }
}

data class WinHardwareInfoData(
  val cpuInfoList: List<WinCpuInfoItem>? = null,
  val diskInfoList: List<WinDiskInfoItem>? = null,
  val gpuInfoList: List<WinGpuInfoItem>? = null,
  val memoryInfoList: List<WinMemoryInfoItem>? = null
)

@Serializable
data class WinCpuInfoItem(
  @SerialName("Name") val name: String,
  @SerialName("NumberOfCores") val cpuCoresNumber: Int,
  @SerialName("NumberOfLogicalProcessors") val cpuLogicalProcessorsNumber: Int
)

@Serializable
data class WinDiskInfoItem(
  @SerialName("Model") val model: String, @SerialName("Size") val size: Long
)

@Serializable
data class WinGpuInfoItem(
  @SerialName("Name") val name: String, @SerialName("AdapterRam") val ram: Long
)

@Serializable
data class WinMemoryInfoItem(
  @SerialName("Manufacturer") val manufacturer: String,
  @SerialName("Capacity") val capacity: Long,
  @SerialName("Speed") val speed: Int
)

object WinHardwareInfo {
  val hardwareInfo by lazy {
    getWinHardwareInfo().also { println("QAQ xxxx $it") }
  }

  val uuid get() = hardwareInfo.uuid

  val cpuInfo
    get() = try {
      if (hardwareInfo.cpuInfo != null) JsonLoose.decodeFromString<List<WinCpuInfoItem>>(
        hardwareInfo.cpuInfo!!
      )
      else null
    } catch (_: Exception) {
      null
    }

  val diskInfo
    get() = try {
      if (hardwareInfo.diskInfo != null) JsonLoose.decodeFromString<List<WinDiskInfoItem>>(
        hardwareInfo.diskInfo!!
      )
      else null
    } catch (_: Exception) {
      null
    }

  val gpuInfo
    get() = try {
      if (hardwareInfo.gpuInfo != null) JsonLoose.decodeFromString<List<WinGpuInfoItem>>(
        hardwareInfo.gpuInfo!!
      )
      else null
    } catch (_: Exception) {
      null
    }

  val memoryInfo
    get() = try {
      if (hardwareInfo.memoryInfo != null) JsonLoose.decodeFromString<List<WinMemoryInfoItem>>(
        hardwareInfo.memoryInfo!!
      )
      else null
    } catch (_: Exception) {
      null
    }

  fun getHostName(): String? {
    if(!PureViewController.isWindows) {
      return null
    }

    val nSize = IntByReference(1024)
    val buffer = CharArray(1024)
    return if (Kernel32.INSTANCE.GetComputerNameEx(
        Kernel32.ComputerNamePhysicalDnsHostname, buffer, nSize
      )
    ) {
      String(buffer, 0, nSize.value)
    } else {
      null
    }
  }

  fun getHardwareInfo(): WinHardwareInfoData? {
    if (!PureViewController.isWindows) {
      return null
    }

    return WinHardwareInfoData(
      cpuInfoList = cpuInfo,
      diskInfoList = diskInfo,
      gpuInfoList = gpuInfo,
      memoryInfoList = memoryInfo
    )
  }
}