package org.dweb_browser.sys.microphone

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermission

val debugMicroPhone = Debugger("MicroPhone")

@Serializable
data class MicroPhoneResult(val success: Boolean, val message: String, val data: String)

class MicroPhoneNMM : NativeMicroModule("microphone.sys.dweb", "camera") {
  private val microPhoneManage = MicroPhoneManage()

  init {
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Process_Service
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/recordSound" bind PureMethod.GET by defineJsonResponse {
        debugMicroPhone("recordSound", "enter")
        val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@MicroPhoneNMM
        val microPhoneResult = if (fromMM.requestSystemPermission(
            SystemPermissionTask(
              name = SystemPermissionName.MICROPHONE,
              title = MicroPhoneI18nResource.request_permission_title.text,
              description = MicroPhoneI18nResource.request_permission_message.text
            )
          )
        ) {
          val data = microPhoneManage.recordSound(fromMM)
          if (data.isNotEmpty()) {
            MicroPhoneResult(true, "Success", data)
          } else {
            MicroPhoneResult(false, MicroPhoneI18nResource.data_is_null.text, "")
          }
        } else {
          MicroPhoneResult(false, MicroPhoneI18nResource.permission_denied.text, "")
        }
        microPhoneResult.toJsonElement()
      },
    )
  }

  override suspend fun _shutdown() {
  }
}