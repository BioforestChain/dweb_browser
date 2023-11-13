package org.dweb_browser.sys.motionSensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.mainAsyncExceptionHandler

actual class MotionSensorsApi actual constructor(mm: NativeMicroModule) : SensorEventListener {
  private val sensorManager =
    mm.getAppContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val accelerometerSignal = Signal<org.dweb_browser.sys.motionSensors.Axis>()
  private var gyroscopeSignal = Signal<org.dweb_browser.sys.motionSensors.Axis>()
  private val mainScope = MainScope()
  private val sensorsRegisterSet = mutableSetOf<Int>()

  actual fun startAccelerometerListener(interval: Int?): Boolean {
    if (sensorsRegisterSet.contains(Sensor.TYPE_ACCELEROMETER)) {
      return true
    }

    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false

    return sensorManager.registerListener(
      this,
      accelerometer,
      interval ?: SensorManager.SENSOR_DELAY_NORMAL
    ).also {
      sensorsRegisterSet.add(Sensor.TYPE_ACCELEROMETER)
    }
  }

  actual fun startGyroscopeListener(interval: Int?): Boolean {
    if (sensorsRegisterSet.contains(Sensor.TYPE_GYROSCOPE)) {
      return true
    }

    val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) ?: return false

    return sensorManager.registerListener(
      this,
      gyroscope,
      interval ?: SensorManager.SENSOR_DELAY_NORMAL
    ).also {
      sensorsRegisterSet.add(Sensor.TYPE_GYROSCOPE)
    }
  }

  actual fun unregisterListener() {
    sensorManager.unregisterListener(this)
    mainScope.cancel()
    accelerometerSignal.clear()
    gyroscopeSignal.clear()
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
      event.apply {
        mainScope.launch(mainAsyncExceptionHandler) {
          accelerometerSignal.emit(
            org.dweb_browser.sys.motionSensors.Axis(
              values[0].toDouble(), values[1].toDouble(), values[2].toDouble()
            )
          )
        }
      }
    } else if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
      event.apply {
        mainScope.launch(mainAsyncExceptionHandler) {
          gyroscopeSignal.emit(
            org.dweb_browser.sys.motionSensors.Axis(
              values[0].toDouble(), values[1].toDouble(), values[2].toDouble()
            )
          )
        }
      }
    }
  }

  // TODO: 精度变化
  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

  actual val onAccelerometerChanges = accelerometerSignal.toListener()


  actual val onGyroscopeChanges = gyroscopeSignal.toListener()
}
