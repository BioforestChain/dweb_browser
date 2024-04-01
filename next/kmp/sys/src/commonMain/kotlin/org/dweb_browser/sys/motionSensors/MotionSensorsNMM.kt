package org.dweb_browser.sys.motionSensors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.pure.http.queryAsOrNull
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.ioAsyncExceptionHandler

class MotionSensorsNMM : NativeMicroModule.NativeRuntime("motion-sensors.sys.dweb", "Motion Sensors") {
  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      // 获取加速计 (push模式)
      "/observe/accelerometer" byChannel { ctx ->
        val fps = request.queryAsOrNull<Double>("fps")
        val motionSensors = MotionSensorsManage(this@MotionSensorsNMM)

        if(motionSensors.isSupportAccelerometer) {
          val job = CoroutineScope(ioAsyncExceptionHandler).launch {
            motionSensors.getAccelerometerFlow(fps).collect {
              ctx.sendJsonLine(it)
            }
          }

          onClose {
            job.cancel()
          }
        }
      },
      // 获取陀螺仪 (push模式)
      "/observe/gyroscope" byChannel { ctx ->
        val motionSensors = MotionSensorsManage(this@MotionSensorsNMM)
        val fps = request.queryAsOrNull<Double>("fps")

        if(motionSensors.isSupportGyroscope) {
          val job = CoroutineScope(ioAsyncExceptionHandler).launch {
            motionSensors.getGyroscopeFlow(fps).collect {
              ctx.sendJsonLine(it)
            }
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
