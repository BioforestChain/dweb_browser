package org.dweb_browser.sys.motionSensors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.dweb_browser.core.module.NativeMicroModule

actual class MotionSensorsManage actual constructor(mm: NativeMicroModule.NativeRuntime) {
  actual val isSupportAccelerometer = false

  actual fun getAccelerometerFlow(fps: Double?): Flow<Axis> {
    return emptyFlow()
  }

  actual val isSupportGyroscope = false

  actual fun getGyroscopeFlow(fps: Double?): Flow<Axis> {
    return emptyFlow()
  }
}