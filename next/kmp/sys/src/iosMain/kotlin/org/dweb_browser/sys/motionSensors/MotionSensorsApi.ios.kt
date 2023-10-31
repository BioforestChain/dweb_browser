package org.dweb_browser.sys.motionSensors

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.mainAsyncExceptionHandler
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

actual class MotionSensorsApi actual constructor(mm: NativeMicroModule) {
  private val motionManager = CMMotionManager()
  private val accelerometerSignal = Signal<Axis>()
  private var gyroscopeSignal = Signal<Axis>()
  private val mainScope = MainScope()

  @OptIn(ExperimentalForeignApi::class)
  actual fun startAccelerometerListener(interval: Int?) : Boolean {
    if (!motionManager.isAccelerometerAvailable()) {
//      throw Exception("设备硬件不支持加速计传感器")
      println("设备硬件不支持加速计传感器")
      return false
    }

    if (!motionManager.isAccelerometerActive()) {
      motionManager.setAccelerometerUpdateInterval(interval.takeIf { it != null }?.toDouble() ?: 0.025)
      motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.currentQueue!!) { accelerometerData, error ->
        accelerometerData?.acceleration?.useContents {
          mainScope.launch(mainAsyncExceptionHandler) {
            accelerometerSignal.emit(Axis(x, y, z))
          }
        }
      }
    }

    return motionManager.accelerometerActive
  }

  actual val onAccelerometerChanges = accelerometerSignal.toListener()

  @OptIn(ExperimentalForeignApi::class)
  actual fun startGyroscopeListener(interval: Int?) : Boolean {
    if (!motionManager.isGyroAvailable()) {
//      throw Exception("设备硬件不支持陀螺仪传感器")
      println("设备硬件不支持陀螺仪传感器")
      return false
    }

    if (!motionManager.isGyroActive()) {
      motionManager.setAccelerometerUpdateInterval(interval.takeIf { it != null }?.toDouble() ?: 0.025)
      motionManager.startGyroUpdatesToQueue(NSOperationQueue.currentQueue!!) { gyroData, error ->
        gyroData?.rotationRate?.useContents {
          mainScope.launch(mainAsyncExceptionHandler) {
            gyroscopeSignal.emit(Axis(x, y, z))
          }
        }
      }
    }

    return motionManager.gyroActive
  }


  actual val onGyroscopeChanges = gyroscopeSignal.toListener()

  actual fun unregisterListener() {
    if (motionManager.isAccelerometerActive()) {
      motionManager.stopAccelerometerUpdates()
    }

    if (motionManager.isGyroAvailable()) {
      motionManager.stopGyroUpdates()
    }
  }
}
