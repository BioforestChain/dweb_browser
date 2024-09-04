package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.uikit.InterfaceOrientation
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.darwin.NSObject

/**
 * 在官方修复 LocalInterfaceOrientation.current 之前，请先用 rememberInterfaceOrientation 做过渡
 */
@OptIn(InternalComposeUiApi::class)
@Composable
fun rememberInterfaceOrientation(): InterfaceOrientation {
  val uiDevice = UIDevice.currentDevice
  val orientationObserver = remember(uiDevice) {
    UIDeviceOrientationObserverWM.getOrPut(uiDevice) {
      UIDeviceOrientationObserver(uiDevice)
    }
  }

  /// 默认不做销毁，持续监控
  if (false) {
    orientationObserver.refs.RefEffect()
  }
  return orientationObserver.safeOrientationFlow.collectAsState(orientationObserver.orientationFlow.value).value
}

private val UIDeviceOrientationObserverWM = WeakHashMap<UIDevice, UIDeviceOrientationObserver>()

@OptIn(InternalComposeUiApi::class)
private class UIDeviceOrientationObserver(val device: UIDevice) : NSObject() {
  val refs = Refs {
    disconnect()
  }
  val orientationFlow = MutableStateFlow(device.orientation().toInterfaceOrientation())

  /**
   * 发现有时候会太快，所以这里做了一些延迟
   */
  @OptIn(FlowPreview::class)
  val safeOrientationFlow = orientationFlow.debounce(150)

  val orientationObserver = NSNotificationCenter.defaultCenter.addObserverForName(
    name = UIDeviceOrientationDidChangeNotification,
    `object` = null,
    queue = null,
    usingBlock = {
      orientationFlow.value = device.orientation.toInterfaceOrientation()
    }
  )

  fun disconnect() {
    NSNotificationCenter.defaultCenter.removeObserver(orientationObserver)
  }
}

@OptIn(InternalComposeUiApi::class)
fun platform.UIKit.UIDeviceOrientation.toInterfaceOrientation(): InterfaceOrientation {
  return InterfaceOrientation.getByRawValue(this.value) ?: InterfaceOrientation.Portrait
}