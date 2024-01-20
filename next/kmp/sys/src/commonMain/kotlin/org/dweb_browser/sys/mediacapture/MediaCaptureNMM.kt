package org.dweb_browser.sys.mediacapture

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermission

val debugMediaCapture = Debugger("MediaCapture")

@Serializable
data class MediaCaptureResult(val success: Boolean, val message: String, val data: String)

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
      "/capture" bind PureMethod.GET by defineJsonResponse {
        val mimeType = request.queryOrNull("mime") ?: "*"
        val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@MediaCaptureNMM
        val mediaCaptureResult = if (mimeType.startsWith("video", true)) {
          captureVideo(fromMM)
        } else if (mimeType.startsWith("image", true)) {
          takePicture(fromMM)
        } else if (mimeType.startsWith("audio", true)) {
          recordAudio(fromMM)
        } else {
          MediaCaptureResult(false, MediaCaptureI18nResource.type_issue.text, "")
        }
        mediaCaptureResult.toJsonElement()
      }
    )
  }

  override suspend fun _shutdown() {
  }

  private suspend fun takePicture(fromMM: MicroModule): MediaCaptureResult {
    debugMediaCapture("takePicture", "enter")
    return if (fromMM.requestSystemPermission(
        SystemPermissionTask(
          name = SystemPermissionName.CAMERA,
          title = MediaCaptureI18nResource.request_permission_title_camera.text,
          description = MediaCaptureI18nResource.request_permission_message_take_picture.text
        )
      )
    ) {
      val data = mediaCaptureManage.takePicture(fromMM)
      if (data.isNotEmpty()) {
        MediaCaptureResult(true, "Success", data)
      } else {
        MediaCaptureResult(false, MediaCaptureI18nResource.data_is_null.text, "")
      }
    } else {
      MediaCaptureResult(false, MediaCaptureI18nResource.permission_denied_take_picture.text, "")
    }
  }

  private suspend fun captureVideo(fromMM: MicroModule): MediaCaptureResult {
    debugMediaCapture("captureVideo", "enter")
    return if (fromMM.requestSystemPermission(
        SystemPermissionTask(
          name = SystemPermissionName.CAMERA,
          title = MediaCaptureI18nResource.request_permission_title_camera.text,
          description = MediaCaptureI18nResource.request_permission_message_take_picture.text
        )
      )
    ) {
      val data = mediaCaptureManage.captureVideo(fromMM)
      if (data.isNotEmpty()) {
        MediaCaptureResult(true, "Success", data)
      } else {
        MediaCaptureResult(false, MediaCaptureI18nResource.data_is_null.text, "")
      }
    } else {
      MediaCaptureResult(
        false,
        MediaCaptureI18nResource.permission_denied_take_picture.text,
        ""
      )
    }
  }

  private suspend fun recordAudio(fromMM: MicroModule): MediaCaptureResult {
    debugMediaCapture("recordSound", "enter")
    return if (fromMM.requestSystemPermission(
        SystemPermissionTask(
          name = SystemPermissionName.MICROPHONE,
          title = MediaCaptureI18nResource.request_permission_message_audio.text,
          description = MediaCaptureI18nResource.request_permission_message_audio.text
        )
      )
    ) {
      val data = mediaCaptureManage.recordSound(fromMM)
      if (data.isNotEmpty()) {
        MediaCaptureResult(true, "Success", data)
      } else {
        MediaCaptureResult(false, MediaCaptureI18nResource.data_is_null.text, "")
      }
    } else {
      MediaCaptureResult(false, MediaCaptureI18nResource.permission_denied_record_audio.text, "")
    }
  }
}