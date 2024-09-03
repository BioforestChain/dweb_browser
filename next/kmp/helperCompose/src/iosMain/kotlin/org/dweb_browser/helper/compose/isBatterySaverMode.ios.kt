package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.Refs
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryLevelDidChangeNotification
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIDeviceBatteryStateDidChangeNotification
import platform.darwin.NSObject

@Composable
actual fun isBatterySaverMode(): Boolean {
  val uiDevice = UIDevice.currentDevice
  val batteryObserver = remember(uiDevice) {
    UIDeviceBatteryObserverWM.getOrPut(uiDevice) {
      BatteryObserver(uiDevice)
    }
  }
  /// 默认不做销毁，持续监控
  if (false) {
    batteryObserver.refs.RefEffect()
  }
  val batteryState by batteryObserver.batteryStateFlow.collectAsState()
  val batteryLevel by batteryObserver.batteryLevelFlow.collectAsState()
  /**
   * 监测电池状态，不插电的情况下，剩余电量20%，进入省电模式
   */
  return batteryState == UIDeviceBatteryState.UIDeviceBatteryStateUnplugged && batteryLevel <= 0.2
}

private val UIDeviceBatteryObserverWM = WeakHashMap<UIDevice, BatteryObserver>()

private class BatteryObserver(val device: UIDevice) : NSObject() {
  init {
    device.batteryMonitoringEnabled = true
  }

  val refs = Refs { disconnect() }

  val batteryStateFlow = MutableStateFlow(device.batteryState)
  val batteryLevelFlow = MutableStateFlow(device.batteryLevel)

  val batteryStateObserver = NSNotificationCenter.defaultCenter.addObserverForName(
    name = UIDeviceBatteryStateDidChangeNotification,
    `object` = null,
    queue = null,
    usingBlock = {
      batteryStateFlow.value = device.batteryState
    }
  )
  val batteryLevelObserver = NSNotificationCenter.defaultCenter.addObserverForName(
    name = UIDeviceBatteryLevelDidChangeNotification,
    `object` = null,
    queue = null,
    usingBlock = {
      batteryLevelFlow.value = device.batteryLevel
    }
  )

  fun disconnect() {
    NSNotificationCenter.defaultCenter.removeObserver(batteryStateObserver)
    NSNotificationCenter.defaultCenter.removeObserver(batteryLevelObserver)
  }
}