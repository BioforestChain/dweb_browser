package org.dweb_browser.sys.motionSensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.getAppContextUnsafe

actual class MotionSensorsManage actual constructor(mm: NativeMicroModule.NativeRuntime) {
  private val sensorManager =
    getAppContextUnsafe().getSystemService(Context.SENSOR_SERVICE) as SensorManager

  actual val isSupportAccelerometer get() = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

  actual fun getAccelerometerFlow(fps: Double?): Flow<Axis> {
    return callbackFlow {
      val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
      val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          event?.apply {
            trySend(
              Axis(
                values[0].toDouble(), values[1].toDouble(), values[2].toDouble()
              )
            )
          }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
      }

      sensorManager.registerListener(
        sensorEventListener,
        accelerometer,
        if (fps != null && fps != 0.0) (1_000000 / fps).toInt() else SensorManager.SENSOR_DELAY_NORMAL
      )

      awaitClose {
        sensorManager.unregisterListener(sensorEventListener)
      }
    }
  }

  actual val isSupportGyroscope get() = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null

  actual fun getGyroscopeFlow(fps: Double?): Flow<Axis> {
    return callbackFlow {
      val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
      val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          event?.apply {
            trySend(
              Axis(
                values[0].toDouble(), values[1].toDouble(), values[2].toDouble()
              )
            )
          }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
      }

      sensorManager.registerListener(
        sensorEventListener,
        gyroscope,
        if (fps != null && fps != 0.0) (1_000000 / fps).toInt() else SensorManager.SENSOR_DELAY_NORMAL
      )

      awaitClose {
        sensorManager.unregisterListener(sensorEventListener)
      }
    }
  }
}
