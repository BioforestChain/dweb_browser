package org.dweb_browser.sys.filechooser

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.platform.ios.SoundRecordManager
import org.dweb_browser.sys.mediacapture.MediaCaptureHandler
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication

actual class FileChooserManage {

  init {
    SystemPermissionAdapterManager.append {
      when (task.name) {
        SystemPermissionName.PHONE -> photoAuthorizationStatus()
        SystemPermissionName.STORAGE -> AuthorizationStatus.GRANTED
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
    microModule: MicroModule, accept: String, multiple: Boolean, limit: Int
  ): List<String> {
//    WARNING("Not yet implemented openFileChooser")
    if (accept.startsWith("image/")) {
      val path = imagePath()
      return listOf(path)
    } else if (accept.startsWith("video/")) {
      val path = videoPath()
      return listOf(path)
    } else if (accept.startsWith("audio/")){
      val path = audioPath()
      return listOf(path)
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

  @OptIn(ExperimentalForeignApi::class)
  private suspend fun audioPath(): String {
    val result = CompletableDeferred<String>()
    val manager = SoundRecordManager()
    withMainContext {
      val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
      val recordController = manager.create()
      manager.completeRecordWithCallback { path ->
        recordController.dismissViewControllerAnimated(true, null)
        if (path != null) {
          result.complete(path)
        } else {
          result.complete("")
        }
      }
      rootController?.presentViewController(recordController,true,null)
    }
    return result.await()
  }
}
