package org.dweb_browser.shared.microService.sys.motionSensors

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
  private val accelerometerSignal = Signal<Axis>()
  private var gyroscopeSignal = Signal<Axis>()
  private val mainScope = MainScope()

  private fun isSensorRegister(type: Int): Boolean = sensorManager.getSensorList(type).size > 0

  actual fun startAccelerometerListener(interval: Int?): Boolean {
    if (!isSensorRegister(Sensor.TYPE_ACCELEROMETER)) {
      sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
        return sensorManager.registerListener(
          this,
          accelerometer,
          interval ?: SensorManager.SENSOR_DELAY_NORMAL
        )
      }
    }

    return false
  }

  actual fun startGyroscopeListener(interval: Int?): Boolean {
    if (!isSensorRegister(Sensor.TYPE_GYROSCOPE)) {
      sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { accelerometer ->
        return sensorManager.registerListener(
          this,
          accelerometer,
          interval ?: SensorManager.SENSOR_DELAY_NORMAL
        )
      }
    }

    return false
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
            Axis(
              values[0].toDouble(), values[1].toDouble(), values[2].toDouble()
            )
          )
        }
      }
    } else if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
      event.apply {
        mainScope.launch(mainAsyncExceptionHandler) {
          gyroscopeSignal.emit(
            Axis(
              values[0].toDouble(), values[1].toDouble(), values[2].toDouble()
            )
          )
        }
      }
    }
  }

  // TODO: 精度变化
  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    TODO("Not yet implemented")
  }

  actual val onAccelerometerChanges = accelerometerSignal.toListener()


  actual val onGyroscopeChanges = gyroscopeSignal.toListener()
}
