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

actual class MotionSensorsManage actual constructor(mm: NativeMicroModule) {
  private val motionManager = CMMotionManager()
  private val accelerometerSignal = Signal<Axis>()
  private var gyroscopeSignal = Signal<Axis>()
  private val mainScope = MainScope()

  @OptIn(ExperimentalForeignApi::class)
  actual fun startAccelerometerListener(fps: Int?): Boolean {
    if (!motionManager.isAccelerometerAvailable()) {
//      throw Exception("设备硬件不支持加速计传感器")
      println("设备硬件不支持加速计传感器")
      return false
    }

    if (!motionManager.isAccelerometerActive()) {
      motionManager.setAccelerometerUpdateInterval(
        if (fps != null) (1 / fps).toDouble() else 0.025
      )
      motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { accelerometerData, error ->
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
  actual fun startGyroscopeListener(fps: Int?): Boolean {
    if (!motionManager.isGyroAvailable()) {
//      throw Exception("设备硬件不支持陀螺仪传感器")
      println("设备硬件不支持陀螺仪传感器")
      return false
    }

    if (!motionManager.isGyroActive()) {
      motionManager.setGyroUpdateInterval(
        if (fps != null) (1 / fps).toDouble() else 0.025
      )
      motionManager.startGyroUpdatesToQueue(NSOperationQueue.mainQueue) { gyroData, error ->
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
