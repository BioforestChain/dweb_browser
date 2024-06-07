package org.dweb_browser.sys.motionSensors

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.collectIn
import org.dweb_browser.pure.http.queryAsOrNull

class MotionSensorsNMM : NativeMicroModule("motion-sensors.sys.dweb", "Motion Sensors") {
  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  inner class MotionSensorsRuntime(override val bootstrapContext: BootstrapContext) :
    NativeRuntime() {

    override suspend fun _bootstrap() {
      routes(
        // 获取加速计 (push模式)
        "/observe/accelerometer" byChannel { ctx ->
          val fps = request.queryAsOrNull<Double>("fps")
          // TODO 这个MotionSensorsManage不能共享吗？
          val motionSensors = MotionSensorsManage(this@MotionSensorsRuntime)

          if (motionSensors.isSupportAccelerometer) {
            val job = motionSensors.getAccelerometerFlow(fps).collectIn(mmScope) {
              ctx.sendJsonLine(it)
            }

            onClose {
              job.cancel()
            }
          }
        },
        // 获取陀螺仪 (push模式)
        "/observe/gyroscope" byChannel { ctx ->
          val motionSensors = MotionSensorsManage(this@MotionSensorsRuntime)
          val fps = request.queryAsOrNull<Double>("fps")

          if (motionSensors.isSupportGyroscope) {
            val job = motionSensors.getGyroscopeFlow(fps).collectIn(mmScope) {
              ctx.sendJsonLine(it)
            }

            onClose {
              job.cancel()
            }
          }
        }).cors()
    }

    override suspend fun _shutdown() {

    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    MotionSensorsRuntime(bootstrapContext)
}
