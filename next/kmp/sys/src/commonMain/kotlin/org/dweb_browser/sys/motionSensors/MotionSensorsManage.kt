package org.dweb_browser.sys.motionSensors

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import org.dweb_browser.core.module.NativeMicroModule

@Serializable
data class Axis(val x: Double, val y: Double, val z: Double)

expect class MotionSensorsManage(mm: NativeMicroModule.NativeRuntime) {
  val isSupportAccelerometer: Boolean
  fun getAccelerometerFlow(fps: Double?): Flow<Axis>
  val isSupportGyroscope: Boolean
  fun getGyroscopeFlow(fps: Double?): Flow<Axis>
}
