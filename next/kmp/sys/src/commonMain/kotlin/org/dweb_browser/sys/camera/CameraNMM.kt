package org.dweb_browser.sys.camera

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermission

val debugCamera = Debugger("camera")

@Serializable
data class CameraResult(val success: Boolean, val message: String, val data: String)

class CameraNMM : NativeMicroModule("camera.sys.dweb", "camera") {
  private val cameraManage = CameraManage()

  init {
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Process_Service
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    debugCamera("_bootstrap", "enter")
    routes(
      "/takePicture" bind PureMethod.GET by defineJsonResponse {
        debugCamera("takePicture", "enter")
        val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@CameraNMM
        val cameraResult = if (fromMM.requestSystemPermission(
            SystemPermissionTask(
              name = SystemPermissionName.CAMERA,
              title = CameraI18nResource.request_permission_title.text,
              description = CameraI18nResource.request_permission_message_take_picture.text
            )
          )
        ) {
          val data = cameraManage.takePicture(fromMM)
          if (data.isNotEmpty()) {
            CameraResult(true, "Success", data)
          } else {
            CameraResult(false, CameraI18nResource.data_is_null.text, "")
          }
        } else {
          CameraResult(false, CameraI18nResource.permission_denied_take_picture.text, "")
        }
        cameraResult.toJsonElement()
      },
      "/captureVideo" bind PureMethod.GET by defineJsonResponse {
        debugCamera("captureVideo", "enter")
        val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@CameraNMM
        val cameraResult = if (fromMM.requestSystemPermission(
            SystemPermissionTask(
              name = SystemPermissionName.CAMERA,
              title = CameraI18nResource.request_permission_title.text,
              description = CameraI18nResource.request_permission_message_take_picture.text
            )
          )
        ) {
          val data = cameraManage.captureVideo(fromMM)
          if (data.isNotEmpty()) {
            CameraResult(true, "Success", data)
          } else {
            CameraResult(false, CameraI18nResource.data_is_null.text, "")
          }
        } else {
          CameraResult(false, CameraI18nResource.permission_denied_take_picture.text, "")
        }
        cameraResult.toJsonElement()
      },
      "/getPhoto" bind PureMethod.GET by defineEmptyResponse {
        debugCamera("getPhoto", "enter")
        try {
          val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@CameraNMM
          val imageOptions = request.queryAs<ImageOptions>()
          cameraManage.getPhoto(fromMM, imageOptions)
        } catch (e: Exception) {

        }
      }
    )
  }

  override suspend fun _shutdown() {
  }
}