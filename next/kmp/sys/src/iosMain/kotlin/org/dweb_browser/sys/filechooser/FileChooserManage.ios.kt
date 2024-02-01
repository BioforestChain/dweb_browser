package org.dweb_browser.sys.filechooser

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.WARNING
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.sys.mediacapture.MediaCaptureHandler
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary

actual class FileChooserManage {

  init {
    SystemPermissionAdapterManager.append {
      when (task.name) {
        SystemPermissionName.PHONE -> photoAuthorizationStatus()
        else -> null
      }
    }
  }

  private suspend fun photoAuthorizationStatus(): AuthorizationStatus {
    val status = when (PHPhotoLibrary.authorizationStatus()) {
      PHAuthorizationStatusAuthorized -> AuthorizationStatus.GRANTED
      PHAuthorizationStatusNotDetermined -> {
        val result = CompletableDeferred<AuthorizationStatus>()
        PHPhotoLibrary.requestAuthorization {
          when (it) {
            PHAuthorizationStatusAuthorized -> result.complete(AuthorizationStatus.GRANTED)
            else -> result.complete(AuthorizationStatus.DENIED)
          }
        }
        return result.await()
      }
      else -> AuthorizationStatus.DENIED
    }
    return status
  }

  actual suspend fun openFileChooser(
    microModule: MicroModule, mimeTypes: String, multiple: Boolean, limit: Int
  ): List<String> {
//    WARNING("Not yet implemented openFileChooser")
    if (mimeTypes.startsWith("image")) {
      val path = imagePath()
      return listOf(path)
    } else if (mimeTypes.startsWith("video")) {
      val path = videoPath()
      return listOf(path)
    } else {
      println("audio")
    }
    return emptyList()
  }

  private suspend fun imagePath(): String {
    val result = CompletableDeferred<String>()
    MediaCaptureHandler().launchPhotoString {
      result.complete(it)
    }
    return result.await()
  }

  private suspend fun videoPath(): String {
    val result = CompletableDeferred<String>()
    MediaCaptureHandler().launchVideoPath {
      result.complete(it)
    }
    return result.await()
  }
}
