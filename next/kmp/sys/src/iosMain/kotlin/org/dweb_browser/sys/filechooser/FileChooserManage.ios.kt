package org.dweb_browser.sys.filechooser

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.platform.ios.SoundRecordManager
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
    microModule: MicroModule.Runtime, accept: String, multiple: Boolean, limit: Int
  ): List<String> {
//    WARNING("Not yet implemented openFileChooser")
    if (isImage(accept)) {
      return imagePath(multiple, limit)
    } else if (isVideo(accept)) {
      return videoPath(multiple, limit)
    } else if (isAudio(accept)){
      val path = audioPath(multiple, limit)
      return path.split(",")
    }
    return emptyList()
  }

  private fun isImage(accept: String): Boolean {

    val imageTypes = arrayOf(".jpg",".png",".jpeg",".webp",".svg",".gif",".bmp")
    if (accept.startsWith("image/")) {
      return true
    }
    if (imageTypes.contains(accept)) {
      return true
    }
    return false
  }

  private fun isVideo(accept: String): Boolean {

    val videoTypes = arrayOf(".mp4",".avi",".mov",".webm",".mpeg")
    if (accept.startsWith("video/")) {
      return true
    }
    if (videoTypes.contains(accept)) {
      return true
    }
    return false
  }

  private fun isAudio(accept: String): Boolean {

    val videoTypes = arrayOf(".mp3",".wav",".ogg",".aac",".flac",".midi")
    if (accept.startsWith("audio/")) {
      return true
    }
    if (videoTypes.contains(accept)) {
      return true
    }
    return false
  }

  private suspend fun imagePath(multiple: Boolean, limit: Int): List<String> {
    val result = CompletableDeferred<List<String>>()
    FilePickerManager().chooseImages(multiple,limit) {
      result.complete(it)
    }
//    MediaCaptureHandler().launchPhotoString {
//      result.complete(it)
//    }
    return result.await()
  }

  private suspend fun videoPath(multiple: Boolean, limit: Int): List<String> {
    val result = CompletableDeferred<List<String>>()
    FilePickerManager().chooseVideos(multiple, limit) {
      result.complete(it)
    }
//    MediaCaptureHandler().launchVideoPath {
//      result.complete(it)
//    }
    return result.await()
  }

  @OptIn(ExperimentalForeignApi::class)
  private suspend fun audioPath(multiple: Boolean, limit: Int): String {
    val result = CompletableDeferred<String>()
//    return ""
    val manager = SoundRecordManager()
    withMainContext {
      val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
      val recordController = manager.createWithMultiple(multiple, limit.toLong())
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
