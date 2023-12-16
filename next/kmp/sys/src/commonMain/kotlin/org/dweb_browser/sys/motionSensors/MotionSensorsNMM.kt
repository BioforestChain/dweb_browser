package org.dweb_browser.sys.motionSensors

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.queryAsOrNull
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule

class MotionSensorsNMM : NativeMicroModule("motion-sensors.sys.dweb", "Motion Sensors") {
  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      // 获取加速计 (push模式)
      "/observe/accelerometer" byChannel { ctx ->
        val fps = request.queryAsOrNull<Int>("fps")
        val motionSensors = MotionSensorsApi(this@MotionSensorsNMM)
        motionSensors.startAccelerometerListener(fps)

        motionSensors.onAccelerometerChanges {
          ctx.sendJsonLine(it)
        }

        onClose {
          motionSensors.unregisterListener()
        }
      },
      // 获取陀螺仪 (push模式)
      "/observe/gyroscope" byChannel { ctx ->
        val motionSensors = MotionSensorsApi(this@MotionSensorsNMM)
        val fps = request.queryAsOrNull<Int>("fps")

        motionSensors.startGyroscopeListener(fps)
        motionSensors.onGyroscopeChanges {
          ctx.sendJsonLine(it)
        }

        onClose {
          motionSensors.unregisterListener()
        }
      }).cors()
  }

  override suspend fun _shutdown() {

  }
}
