package org.dweb_browser.sys.mediacapture

import io.ktor.utils.io.ByteReadChannel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.NSInputStreamToByteReadChannel
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionDenied
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.UIKit.UIApplication
import org.dweb_browser.platform.ios.SoundRecordManager
import platform.Foundation.NSInputStream
import platform.Foundation.NSURL

actual class MediaCaptureManage actual constructor() {
  init {
    SystemPermissionAdapterManager.append {
      when (task.name) {
        SystemPermissionName.CAMERA -> cameraAuthorizationStatus()
        SystemPermissionName.MICROPHONE -> microphoneAuthorizationStatus()
        else -> null
      }
    }
  }

  private suspend fun cameraAuthorizationStatus(): AuthorizationStatus {
    val status = when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
      AVAuthorizationStatusAuthorized -> AuthorizationStatus.GRANTED
      AVAuthorizationStatusNotDetermined -> {
        val result = CompletableDeferred<AuthorizationStatus>()
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
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

  private suspend fun microphoneAuthorizationStatus(): AuthorizationStatus {
    val status = when (AVAudioApplication.sharedInstance.recordPermission) {
      AVAudioApplicationRecordPermissionDenied -> AuthorizationStatus.DENIED
      AVAudioApplicationRecordPermissionGranted -> AuthorizationStatus.GRANTED
      else -> {
        val result = CompletableDeferred<AuthorizationStatus>()
        AVAudioApplication.requestRecordPermissionWithCompletionHandler { success ->
          if (success) {
            result.complete(AuthorizationStatus.GRANTED)
          } else {
            result.complete(AuthorizationStatus.DENIED)
          }
        }
        return result.await()
      }
    }
    return status
  }

  actual suspend fun takePicture(microModule: MicroModule): PureStream? {
    val result = CompletableDeferred<ByteArray>()
    MediaCaptureHandler().launchCameraString {
      result.complete(it)
    }
    return PureStream(result.await())
  }

  actual suspend fun captureVideo(microModule: MicroModule): PureStream? {
    val result = CompletableDeferred<ByteReadChannel>()
    withMainContext {
      val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
      val videoController = MediaVideoViewController()
      val coroutineScope =
        CoroutineScope(CoroutineName("video-stream") + ioAsyncExceptionHandler)
      videoController.videoPathBlock = {
        if (it.isNotEmpty()) {
          val url = NSURL.fileURLWithPath(it)
          val inputStream = NSInputStream(url)
          val byteChannel = NSInputStreamToByteReadChannel(coroutineScope, inputStream)
          result.complete(byteChannel)
        } else {
          result.complete(ByteReadChannel.Empty)
        }
      }
      rootController?.presentViewController(videoController,true,null)
    }
    return PureStream(result.await())
  }

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun recordSound(microModule: MicroModule): PureStream? {
    val result = CompletableDeferred<ByteReadChannel>()
    val manager = SoundRecordManager()
    val coroutineScope =
      CoroutineScope(CoroutineName("record-stream") + ioAsyncExceptionHandler)
    withMainContext {
      val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
      val recordController = manager.createRecordController()//manager.create()
      manager.completeSingleRecordWithCallback { path ->
          recordController.dismissViewControllerAnimated(true, null)
        if (!path.isNullOrEmpty()) {
          val url = NSURL.fileURLWithPath(path)
          val inputStream = NSInputStream(url)
          val byteChannel = NSInputStreamToByteReadChannel(coroutineScope, inputStream)
          result.complete(byteChannel)
        } else {
          result.complete(ByteReadChannel(""))
        }
      }
      rootController?.presentViewController(recordController,true,null)
    }
    return PureStream(result.await())
  }
}
