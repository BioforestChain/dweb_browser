package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.platform.ios.KeyValueObserverProtocol
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.darwin.NSObject

@Composable
actual fun isBatterySaverMode(): Boolean {
  val batteryObserver = remember {
    UIDevice.currentDevice.let { device ->
      UIDeviceBatteryObserverWM.getOrPut(device) {
        BatteryObserver(device)
      }
    }
  }
  val batteryState by batteryObserver.batteryStateFlow.collectAsState()
  val batteryLevel by batteryObserver.batteryLevelFlow.collectAsState()
  /**
   * 监测电池状态，不插电的情况下，剩余电量20%，进入省电模式
   */
  return batteryState == UIDeviceBatteryState.UIDeviceBatteryStateUnplugged && batteryLevel <= 0.2
}

private val UIDeviceBatteryObserverWM = WeakHashMap<UIDevice, BatteryObserver>()

@OptIn(ExperimentalForeignApi::class)
private class BatteryObserver(val device: UIDevice) : NSObject(), KeyValueObserverProtocol {
  init {
    device.batteryMonitoringEnabled = true
    device.addObserver(
      observer = this,
      forKeyPath = "batteryState",
      options = NSKeyValueObservingOptionNew,
      context = null
    )
    device.addObserver(
      observer = this,
      forKeyPath = "batteryLevel",
      options = NSKeyValueObservingOptionNew,
      context = null
    )
  }

  val batteryStateFlow = MutableStateFlow(device.batteryState)
  val batteryLevelFlow = MutableStateFlow(device.batteryLevel)

  override fun observeValueForKeyPath(
    keyPath: String?,
    ofObject: Any?,
    change: Map<Any?, *>?,
    context: COpaquePointer?,
  ) {
    if (keyPath == "batteryState") {
      val batteryState = change?.get("new") as UIDeviceBatteryState
      batteryStateFlow.value = batteryState
    } else if (keyPath == "batteryLevel") {
      val batteryLevel = change?.get("new") as Float
      batteryLevelFlow.value = batteryLevel
    }
  }

  fun disconnect() {
    device.removeObserver(
      observer = this,
      forKeyPath = "batteryState",
      context = null
    )
    device.removeObserver(
      observer = this,
      forKeyPath = "batteryLevel",
      context = null
    )
  }
}