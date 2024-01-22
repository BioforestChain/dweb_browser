package org.dweb_browser.sys.mediacapture

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.core.toByteArray
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.ext.requestSystemPermission

val debugMediaCapture = Debugger("MediaCapture")

class MediaCaptureNMM : NativeMicroModule("media-capture.sys.dweb", "MediaCapture") {
  private val mediaCaptureManage = MediaCaptureManage()

  init {
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Process_Service
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /**
       * /capture?mime=* 提供相应的系统选择器，目前支持：音频、视频、照片 三种媒体捕捉
       */
      "/capture" bind PureMethod.GET by definePureStreamHandler {
        val mimeType = request.queryOrNull("mime") ?: "*"
        val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@MediaCaptureNMM

        if (mimeType.startsWith("video", true)) {
          captureVideo(fromMM)
        } else if (mimeType.startsWith("image", true)) {
          takePicture(fromMM)
        } else if (mimeType.startsWith("audio", true)) {
          recordAudio(fromMM)
        } else {
          PureResponse(
            status = HttpStatusCode.NotAcceptable,
            body = PureStreamBody(MediaCaptureI18nResource.type_issue.text.toByteArray())
          ).stream()
        }
      }
    )
  }

  override suspend fun _shutdown() {
  }

  private suspend fun takePicture(fromMM: MicroModule): PureStream {
    debugMediaCapture("takePicture", "enter")
    return if (fromMM.requestSystemPermission(
        name = SystemPermissionName.CAMERA,
        title = MediaCaptureI18nResource.request_permission_title_camera.text,
        description = MediaCaptureI18nResource.request_permission_message_take_picture.text
      )
    ) {
      mediaCaptureManage.takePicture(fromMM) ?: PureResponse(
        status = HttpStatusCode.NotFound,
        body = PureStreamBody(MediaCaptureI18nResource.capture_no_found_picture.text.toByteArray())
      ).stream()
    } else {
      PureResponse(
        status = HttpStatusCode.Unauthorized,
        body = PureStreamBody(MediaCaptureI18nResource.permission_denied_take_picture.text.toByteArray())
      ).stream()
    }
  }

  private suspend fun captureVideo(fromMM: MicroModule): PureStream {
    debugMediaCapture("captureVideo", "enter")
    return if (fromMM.requestSystemPermission(
        name = SystemPermissionName.CAMERA,
        title = MediaCaptureI18nResource.request_permission_title_camera.text,
        description = MediaCaptureI18nResource.request_permission_message_take_picture.text
      )
    ) {
      mediaCaptureManage.captureVideo(fromMM) ?: PureResponse(
        status = HttpStatusCode.NotFound,
        body = PureStreamBody(MediaCaptureI18nResource.capture_no_found_video.text.toByteArray())
      ).stream()
    } else {
      PureResponse(
        status = HttpStatusCode.Unauthorized,
        body = PureStreamBody(MediaCaptureI18nResource.permission_denied_capture_video.text.toByteArray())
      ).stream()
    }
  }

  private suspend fun recordAudio(fromMM: MicroModule): PureStream {
    debugMediaCapture("recordSound", "enter")
    return if (fromMM.requestSystemPermission(
        name = SystemPermissionName.MICROPHONE,
        title = MediaCaptureI18nResource.request_permission_message_audio.text,
        description = MediaCaptureI18nResource.request_permission_message_audio.text
      )
    ) {
      mediaCaptureManage.recordSound(fromMM) ?: PureResponse(
        status = HttpStatusCode.NotFound,
        body = PureStreamBody(MediaCaptureI18nResource.capture_no_found_audio.text.toByteArray())
      ).stream()
    } else {
      PureResponse(
        status = HttpStatusCode.Unauthorized,
        body = PureStreamBody(MediaCaptureI18nResource.permission_denied_record_audio.text.toByteArray())
      ).stream()
    }
  }
}