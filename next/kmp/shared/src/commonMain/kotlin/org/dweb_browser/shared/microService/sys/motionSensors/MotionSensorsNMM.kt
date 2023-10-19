package org.dweb_browser.shared.microService.sys.motionSensors

import io.ktor.http.HttpMethod
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule

class MotionSensorsNMM : NativeMicroModule("motion-sensors.sys.dweb", "Motion Sensors") {
  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val msNMM = this
    routes(
      // 获取加速计 (push模式)
      "/observe/accelerometer" bind HttpMethod.Get to defineJsonLineResponse {
        val interval = request.queryAsOrNull<Int>("interval")
        val motionSensors = MotionSensorsApi(msNMM)
        motionSensors.startAccelerometerListener(interval)

        motionSensors.onAccelerometerChanges {
          emit(it)
        }

        onDispose {
          motionSensors.unregisterListener()
        }
      },
      // 获取陀螺仪 (push模式)
      "/observe/gyroscope" bind HttpMethod.Get to defineJsonLineResponse {
        val motionSensors = MotionSensorsApi(msNMM)
        val interval = request.queryAsOrNull<Int>("interval")

        motionSensors.startGyroscopeListener(interval)
        motionSensors.onGyroscopeChanges {
          emit(it)
        }

        onDispose {
          motionSensors.unregisterListener()
        }
      }).cors()
  }

  override suspend fun _shutdown() {

  }
}
