package org.dweb_browser.sys.camera

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType

actual class CameraManage actual constructor() {
  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.CAMERA) {
        cameraAuthorizationStatus()
      } else null
    }
  }

  private suspend fun cameraAuthorizationStatus() : AuthorizationStatus {
    val status = when(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
      AVAuthorizationStatusAuthorized -> AuthorizationStatus.GRANTED
      AVAuthorizationStatusNotDetermined -> {
        val result = CompletableDeferred<AuthorizationStatus>()
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) {granted ->
          if (granted) {
            result.complete(AuthorizationStatus.GRANTED)
          } else {
            result.complete(AuthorizationStatus.DENIED)
          }
        }
        return result.await()
      }
      else -> AuthorizationStatus.DENIED
    }
    return status
  }

  actual suspend fun takePicture(microModule: MicroModule): String {
    WARNING("Not yet implemented takePicture")
    return ""
  }

  actual suspend fun captureVideo(microModule: MicroModule): String {
    WARNING("Not yet implemented captureVideo")
    return ""
  }

}
