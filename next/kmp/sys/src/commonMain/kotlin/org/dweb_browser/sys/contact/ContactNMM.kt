package org.dweb_browser.sys.contact

import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.ext.requestSystemPermission

val debugContact = Debugger("ContactPicker")

class ContactNMM : NativeMicroModule("contact-picker.sys.dweb", "MediaCapture") {
  private val contactManage = ContactManage()

  init {
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Process_Service
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/pickContact" bind PureMethod.GET by defineEmptyResponse {
        debugContact("pickContact", "enter")
        val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@ContactNMM
        if (requestSystemPermission(
            name = SystemPermissionName.CONTACTS,
            title = ContactI18nResource.request_permission_title_contact.text,
            description = ContactI18nResource.request_permission_message_pick_contact.text
          )
        ) {
          contactManage.pickContact(fromMM) ?: throwException(HttpStatusCode.NotFound)
        } else {
          throwException(
            HttpStatusCode.Unauthorized,
            ContactI18nResource.permission_denied_pick_contact.text
          )
        }
      }
    )
  }

  override suspend fun _shutdown() {
  }
}