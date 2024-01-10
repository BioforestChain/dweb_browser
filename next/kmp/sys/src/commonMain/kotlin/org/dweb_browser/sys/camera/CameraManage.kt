package org.dweb_browser.sys.camera

import org.dweb_browser.core.module.MicroModule

expect class CameraManage() {
  fun cameraPermission(): Boolean

  suspend fun takePicture(microModule: MicroModule): String
  suspend fun captureVideo(microModule: MicroModule): String
}