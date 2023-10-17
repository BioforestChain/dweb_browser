package org.dweb_browser.shared.microService.sys.motionSensors

import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Signal

actual class MotionSensorsApi actual constructor(mm: NativeMicroModule) {
  private val accelerometerSignal = Signal<Axis>()
  private var gyroscopeSignal = Signal<Axis>()
  actual fun startAccelerometerListener(interval: Int?) : Boolean {
    return false
  }

  actual val onAccelerometerChanges = accelerometerSignal.toListener()

  actual fun startGyroscopeListener(interval: Int?): Boolean {
    TODO("Not yet implemented")
  }

  actual val onGyroscopeChanges = gyroscopeSignal.toListener()

  actual fun unregisterListener() {
  }
}
