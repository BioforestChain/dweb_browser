package org.dweb_browser.sys.notification

import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.ext.requestSystemPermission

class NotificationNMM : NativeMicroModule("notification.sys.dweb", "notification") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service)
    dweb_permissions = listOf(
      DwebPermission(
        pid = "$mmid/create",
        routes = listOf("file://$mmid/create"),
        title = NotificationI18nResource.apply_notification_permission.text,
      )
    )
  }

  inner class NotificationRuntime(override val bootstrapContext: BootstrapContext) :
    NativeRuntime() {

    override suspend fun _bootstrap() {
      val notificationManager = NotificationManager()
      routes(
        /** 创建消息*/
        "/create" bind PureMethod.GET by definePureResponse {
          val messageItem = request.queryAs<NotificationWebItem>()
          val fromMM = getRemoteRuntime()
          val grant = requestSystemPermission(
            name = SystemPermissionName.Notification,
            title = NotificationI18nResource.request_permission_title.text,
            description = NotificationI18nResource.request_permission_message.text
          )
          if (grant) {
            notificationManager.createNotification(fromMM, messageItem)
            PureResponse(HttpStatusCode.OK)
          } else {
            PureResponse(HttpStatusCode.NotAcceptable)
          }
        },
      )
    }

    override suspend fun _shutdown() {

    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    NotificationRuntime(bootstrapContext)
}