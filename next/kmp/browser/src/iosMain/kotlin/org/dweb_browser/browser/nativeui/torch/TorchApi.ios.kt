package org.dweb_browser.browser.nativeui.torch

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.hasTorch
import platform.AVFoundation.torchActive
import platform.AVFoundation.torchMode

actual object TorchApi {
  private val videoCaptureDevice = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)

  @OptIn(ExperimentalForeignApi::class)
  actual fun toggleTorch() {
    if (videoCaptureDevice?.hasTorch() == true) {
      if (videoCaptureDevice.lockForConfiguration(null)) {
        videoCaptureDevice.torchMode =
          if (videoCaptureDevice.torchActive) AVCaptureTorchModeOff else AVCaptureTorchModeOn
      }

      videoCaptureDevice.unlockForConfiguration()
    }
  }

  actual fun torchState(): Boolean = videoCaptureDevice?.torchActive ?: false
}
