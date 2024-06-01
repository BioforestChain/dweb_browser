package org.dweb_browser.sys.mediacapture

import io.ktor.utils.io.ByteReadChannel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.plus
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.NSInputStreamToByteReadChannel
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.platform.ios.SoundRecordManager
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.sys.permission.RequestSystemPermission
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
import platform.Foundation.NSInputStream
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual class MediaCaptureManage actual constructor() {
  companion object {
    internal val cameraPermission: RequestSystemPermission = {
      if (task.name == SystemPermissionName.CAMERA) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
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
            result.await()
          }

          else -> AuthorizationStatus.DENIED
        }
      } else {
        null
      }
    }

    internal val microPhonePermission: RequestSystemPermission = {
      if (task.name == SystemPermissionName.MICROPHONE) {
        when (AVAudioApplication.sharedInstance.recordPermission) {
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
            result.await()
          }
        }
      } else {
        null
      }
    }
  }

  actual suspend fun takePicture(microModule: MicroModule.Runtime): PureStream? {
    val result = CompletableDeferred<ByteArray>()
    MediaCaptureHandler().launchCameraString {
      result.complete(it)
    }
    return PureStream(result.await())
  }

  actual suspend fun captureVideo(microModule: MicroModule.Runtime): PureStream? {
    val result = CompletableDeferred<ByteReadChannel>()
    withMainContext {
      val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
      val videoController = MediaVideoViewController()
      val coroutineScope = globalDefaultScope + CoroutineName("video-stream")
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
      rootController?.presentViewController(videoController, true, null)
    }
    return PureStream(result.await())
  }

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun recordSound(microModule: MicroModule.Runtime): PureStream? {
    val result = CompletableDeferred<ByteReadChannel>()
    val manager = SoundRecordManager()
    val coroutineScope = globalDefaultScope + CoroutineName("record-stream")
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
      rootController?.presentViewController(recordController, true, null)
    }
    return PureStream(result.await())
  }
}
