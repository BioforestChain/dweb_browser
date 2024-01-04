package org.dweb_browser.sys.motionSensors

import kotlinx.serialization.Serializable
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Signal

@Serializable
data class Axis(val x: Double, val y: Double, val z: Double)

expect class MotionSensorsManage(mm: NativeMicroModule) {
  fun startAccelerometerListener(fps: Int?): Boolean
  val onAccelerometerChanges: Signal.Listener<Axis>
  fun startGyroscopeListener(fps: Int?): Boolean
  val onGyroscopeChanges: Signal.Listener<Axis>
  fun unregisterListener()
}
