package org.dweb_browser.sys.contact

import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.ext.requestSystemPermission

val debugContact = Debugger("ContactPicker")

class ContactNMM : NativeMicroModule("contact-picker.sys.dweb", "ContactPicker") {
  private val contactManage = ContactManage()

  init {
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Process_Service
    )
  }

  inner class ContactRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {


    override suspend fun _bootstrap() {
      routes(
        "/pickContact" bind PureMethod.GET by defineJsonResponse {
          debugContact("pickContact", "enter")
          val fromMM = getRemoteRuntime()
          if (requestSystemPermission(
              name = SystemPermissionName.CONTACTS,
              title = ContactI18nResource.request_permission_title_contact.text,
              description = ContactI18nResource.request_permission_message_pick_contact.text
            )
          ) {
            contactManage.pickContact(fromMM)?.toJsonElement()
              ?: throwException(HttpStatusCode.NotFound)
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

  override fun createRuntime(bootstrapContext: BootstrapContext) = ContactRuntime(bootstrapContext)
}