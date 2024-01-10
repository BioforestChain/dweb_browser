package org.dweb_browser.sys.camera

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.pure.http.PureMethod

val debugCamera = Debugger("camera")

class CameraNMM : NativeMicroModule("camera.sys.dweb", "camera") {
  private val cameraManage = CameraManage()

  init {
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Process_Service
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    debugCamera("_bootstrap", "enter")
    routes(
      "/takePicture" bind PureMethod.GET by defineStringResponse {
        debugCamera("takePicture", "enter")
        val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@CameraNMM
        cameraManage.takePicture(fromMM)
      },
      "/captureVideo" bind PureMethod.GET by defineStringResponse {
        debugCamera("captureVideo", "enter")
        val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@CameraNMM
        cameraManage.captureVideo(fromMM)
      }
    )
  }

  override suspend fun _shutdown() {
  }
}