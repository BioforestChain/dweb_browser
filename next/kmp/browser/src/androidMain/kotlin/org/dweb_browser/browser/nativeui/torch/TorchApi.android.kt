package org.dweb_browser.browser.nativeui.torch

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import org.dweb_browser.helper.getAppContextUnsafe

actual object TorchApi {
  // 初始化上下文
  private var cameraManager: CameraManager =
    getAppContextUnsafe().getSystemService(Context.CAMERA_SERVICE) as CameraManager
  private var cameraId: String? = cameraManager.cameraIdList[0]
  private var isTorchOn: Boolean = false
  actual fun toggleTorch() {
    cameraId?.let {
      try {
        cameraManager.setTorchMode(it, !isTorchOn)
        isTorchOn = !isTorchOn
      } catch (e: CameraAccessException) {
        e.printStackTrace()
      }
    }
  }

  actual fun torchState(): Boolean {
    return isTorchOn
  }
}


