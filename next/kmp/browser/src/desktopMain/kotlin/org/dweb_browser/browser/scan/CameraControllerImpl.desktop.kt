package org.dweb_browser.browser.scan

import org.dweb_browser.helper.WARNING


class CameraControllerImpl(private val controller: SmartScanController) : CameraController {

  init {
//    Webcam.setDriver(WebcamCompositeDriver(ScreenCaptureDriver()))
  }

  fun startCapture() {
  }

  override fun openAlbum() {
    controller.updatePreviewType(SmartModuleTypes.Album)
  }

  override fun stop() {
  }

  override fun toggleTorch() {
    WARNING("Desktop side does not support flash！")
  }
}