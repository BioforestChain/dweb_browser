package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.randomUUID
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryLevelDidChangeNotification
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIDeviceBatteryStateDidChangeNotification
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
  /// 默认不做销毁，持续监控
  if (false) {
    val reason = remember { randomUUID() }
    DisposableEffect(reason) {
      batteryObserver.ref.add(reason)
      onDispose {
        batteryObserver.ref.remove(reason)
        batteryObserver.disconnect()
        UIDeviceBatteryObserverWM.remove(batteryObserver.device)
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

private class BatteryObserver(val device: UIDevice) : NSObject() {
  init {
    device.batteryMonitoringEnabled = true
  }

  val ref = SafeHashSet<String>()

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