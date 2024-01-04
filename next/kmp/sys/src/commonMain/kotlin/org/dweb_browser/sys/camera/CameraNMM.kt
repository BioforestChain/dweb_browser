package org.dweb_browser.sys.camera

import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule

class CameraNMM: NativeMicroModule("camera.sys.dweb", "camera") {
  val cameraManage = CameraManage()

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
  }

  override suspend fun _shutdown() {
  }
}