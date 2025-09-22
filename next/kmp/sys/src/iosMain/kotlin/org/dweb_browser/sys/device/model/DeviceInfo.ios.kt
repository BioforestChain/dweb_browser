package org.dweb_browser.sys.device.model

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import org.dweb_browser.helper.toFixed
import org.dweb_browser.helper.toSpaceSize
import platform.Foundation.NSProcessInfo
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.posix.uname
import platform.posix.utsname

data class BatteryInfo(
  val electricity: String,
  val isCharging: Boolean
)

object DeviceInfo {
  @OptIn(ExperimentalForeignApi::class)
  val model: String
    get() = memScoped {
      val systemInfo = alloc<utsname>()
      if (uname(systemInfo.ptr) == 0) {
        return@memScoped systemInfo.machine.toKString()
      } else {
        UIDevice.currentDevice.model
      }
    }

  val modelName: String
    get() = modelMatch(model)

  val deviceName: String
    get() = UIDevice.currentDevice.name

  val brightness: String
    get() = "${(UIScreen.mainScreen.brightness * 100).toFixed(1)}%"

  val hasDynamicIsland: Boolean
    get() = extractNumberFromString(model).toInt() >= 15

  val ram: String
    get() = NSProcessInfo.processInfo.physicalMemory.toLong().toSpaceSize()

  val gpu: String
    get() = MTLCreateSystemDefaultDevice()?.name ?: "Unknown"

  val cpuCoresNumber: Int
    get() = NSProcessInfo.processInfo.processorCount.toInt()

  val batteryInfo: BatteryInfo
    get() {
      UIDevice.currentDevice.run {
        // 开启电池电量监听，否则永远是-1.0
        batteryMonitoringEnabled = true

        val electricity = if (batteryLevel >= 0) {
          "${(batteryLevel * 100).toInt()}%"
        } else {
          "Unknown"
        }

        return BatteryInfo(electricity, this.batteryState.value != 1L)
      }
    }

  private fun modelMatch(model: String): String = when (model) {
    "iPhone11,2" -> "iPhone XS"
    "iPhone11,4" -> "iPhone XSMax (China)"
    "iPhone11,6" -> "iPhone XSMax"
    "iPhone11,8" -> "iPhone XR"
    "iPhone12,1" -> "iPhone 11"
    "iPhone12,3" -> "iPhone 11 Pro"
    "iPhone12,5" -> "iPhone 11 Pro Max"
    "iPhone12,8" -> "iPhone SE (2nd generation)"
    "iPhone13,1" -> "iPhone 12 mini"
    "iPhone13,2" -> "iPhone 12"
    "iPhone13,3" -> "iPhone 12 Pro"
    "iPhone13,4" -> "iPhone 12 Pro Max"
    "iPhone14,4" -> "iPhone 13 mini"
    "iPhone14,5" -> "iPhone 13"
    "iPhone14,2" -> "iPhone 13 Pro"
    "iPhone14,3" -> "iPhone 13 Pro Max"
    "iPhone14,6" -> "iPhone SE (3rd generation)"
    "iPhone14,7" -> "iPhone 14"
    "iPhone14,8" -> "iPhone 14 Plus"
    "iPhone15,2" -> "iPhone 14 Pro"
    "iPhone15,3" -> "iPhone 14 Pro Max"
    "iPhone15,4" -> "iPhone 15"
    "iPhone15,5" -> "iPhone 15 Plus"
    "iPhone16,1" -> "iPhone 15 Pro"
    "iPhone16,2" -> "iPhone 15 Pro Max"
    "iPhone17,3" -> "iPhone 16"
    "iPhone17,4" -> "iPhone 16 Plus"
    "iPhone17,1" -> "iPhone 16 Pro"
    "iPhone17,2" -> "iPhone 16 Pro Max"
    "iPhone17,5" -> "iPhone 16e"
    "iPhone18,1" -> "iPhone 17 Pro"
    "iPhone18,2" -> "iPhone 17 Pro Max"
    "iPhone18,3" -> "iPhone 17"
    "iPhone18,4" -> "iPhone 17 Air"
    else -> "Unknown"
  }

  private fun extractNumberFromString(input: String): String {
    val prefix = input.takeWhile { it.isLetter() }
    val rest = input.removePrefix(prefix)
    val firstNumber = rest.takeWhile { it.isDigit() }
    return firstNumber
  }
}