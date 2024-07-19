package org.dweb_browser.sys.mediacapture

import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.IHandlerContext
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.ext.requestSystemPermission

val debugMediaCapture = Debugger("MediaCapture")

class MediaCaptureNMM : NativeMicroModule("media-capture.sys.dweb", "MediaCapture") {

  init {
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Process_Service
    )
  }

  inner class MediaCaptureRuntime(override val bootstrapContext: BootstrapContext) :
    NativeRuntime() {

    private val mediaCaptureManage = MediaCaptureManage()

    override suspend fun _bootstrap() {
      routes(/**
       * /capture?mime=* 提供相应的系统选择器，目前支持：音频、视频、照片 三种媒体捕捉
       */
        "/capture" bind PureMethod.GET by definePureStreamHandler {
          val mimeType = request.queryOrNull("mime") ?: "*"
          val fromMM = getRemoteRuntime()

          if (mimeType.startsWith("video", true)) {
            captureVideo(fromMM)
          } else if (mimeType.startsWith("image", true)) {
            takePicture(fromMM)
          } else if (mimeType.startsWith("audio", true)) {
            recordAudio(fromMM)
          } else {
            throwException(HttpStatusCode.NotAcceptable, MediaCaptureI18nResource.type_issue.text)
          }
        })
    }

    override suspend fun _shutdown() {}

    private suspend fun IHandlerContext.takePicture(fromMM: MicroModule.Runtime): PureStream {
      debugMediaCapture("takePicture", "enter")
      return if (fromMM.requestSystemPermission(
          name = SystemPermissionName.CAMERA,
          title = MediaCaptureI18nResource.request_permission_title_camera.text,
          description = MediaCaptureI18nResource.request_permission_message_take_picture.text
        )
      ) {
        mediaCaptureManage.takePicture(fromMM) ?: throwException(
          HttpStatusCode.NotFound, MediaCaptureI18nResource.capture_no_found_picture.text
        )
      } else {
        throwException(
          HttpStatusCode.Unauthorized, MediaCaptureI18nResource.permission_denied_take_picture.text
        )
      }
    }

    private suspend fun IHandlerContext.captureVideo(fromMM: MicroModule.Runtime): PureStream {
      debugMediaCapture("captureVideo", "enter")
      return if (fromMM.requestSystemPermission(
          name = SystemPermissionName.CAMERA,
          title = MediaCaptureI18nResource.request_permission_title_camera.text,
          description = MediaCaptureI18nResource.request_permission_message_take_picture.text
        )
      ) {
        mediaCaptureManage.captureVideo(fromMM) ?: throwException(
          HttpStatusCode.NotFound, MediaCaptureI18nResource.capture_no_found_video.text
        )
      } else {
        throwException(
          HttpStatusCode.Unauthorized, MediaCaptureI18nResource.permission_denied_capture_video.text
        )
      }
    }

    private suspend fun IHandlerContext.recordAudio(fromMM: MicroModule.Runtime): PureStream {
      debugMediaCapture("recordSound", "enter")
      return if (fromMM.requestSystemPermission(
          name = SystemPermissionName.MICROPHONE,
          title = MediaCaptureI18nResource.request_permission_message_audio.text,
          description = MediaCaptureI18nResource.request_permission_message_audio.text
        )
      ) {
        mediaCaptureManage.recordSound(fromMM) ?: throwException(
          HttpStatusCode.NotFound, MediaCaptureI18nResource.capture_no_found_audio.text
        )
      } else {
        throwException(
          HttpStatusCode.Unauthorized, MediaCaptureI18nResource.permission_denied_record_audio.text
        )
      }
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    MediaCaptureRuntime(bootstrapContext)
}