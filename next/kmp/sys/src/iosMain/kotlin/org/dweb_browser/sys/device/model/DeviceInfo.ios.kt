package org.dweb_browser.sys.device.model

import platform.UIKit.UIDevice

data class BatteryInfo(
  val batteryLevel: Float,
  val isCharging: Boolean
)


object DeviceInfo {
  val model: String
    get() = UIDevice.currentDevice.model.also { println("QAQ model=${it}") }

  val modelName: String
    get() = modelMatch(model)

  val deviceName: String
    get() = UIDevice.currentDevice.name

  val batteryInfo: BatteryInfo
    get() {
      UIDevice.currentDevice.run {
        return BatteryInfo(batteryLevel, this.batteryState.value != 1L)
      }
    }

  private fun modelMatch(model: String): String = when (model) {
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
    else -> "Unknown"
  }
}