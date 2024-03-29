package org.dweb_browser.sys.motionSensors

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.dweb_browser.core.module.NativeMicroModule
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

actual class MotionSensorsManage actual constructor(mm: NativeMicroModule.NativeRuntime) {
  private val motionManager = CMMotionManager()

  actual val isSupportAccelerometer get() = motionManager.isAccelerometerAvailable()

  @OptIn(ExperimentalForeignApi::class)
  actual fun getAccelerometerFlow(fps: Double?): Flow<Axis> {
    return callbackFlow {
      if (!motionManager.isAccelerometerActive()) {
        motionManager.setAccelerometerUpdateInterval(
          if (fps != null && fps != 0.0) (1 / fps) else 0.025
        )
        motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { accelerometerData, error ->
          accelerometerData?.acceleration?.useContents {
            trySend(Axis(x, y, z))
          }
        }

        awaitClose {
          motionManager.stopAccelerometerUpdates()
        }
      }

      close()
    }
  }

  actual val isSupportGyroscope get() = motionManager.isGyroAvailable()

  @OptIn(ExperimentalForeignApi::class)
  actual fun getGyroscopeFlow(fps: Double?): Flow<Axis> {
    return callbackFlow {
      if (!motionManager.isGyroActive()) {
        motionManager.setGyroUpdateInterval(
          if (fps != null && fps != 0.0) (1 / fps) else 0.025
        )
        motionManager.startGyroUpdatesToQueue(NSOperationQueue.mainQueue) { gyroData, error ->
          gyroData?.rotationRate?.useContents {
            trySend(Axis(x, y, z))
          }
        }

        awaitClose {
          motionManager.stopGyroUpdates()
        }
      }

      close()
    }
  }
}
