package org.dweb_browser.sys.notification

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.queryAs
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule

class NotificationNMM : NativeMicroModule("notification.sys.dweb", "notification") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val notificationManager = NotificationManager()
    routes(
      /** 创建消息*/
      "/create" bind HttpMethod.Get by definePureResponse {
        val messageItem = request.queryAs<NotificationMsgItem>()
        notificationManager.createNotification(messageItem)
        PureResponse(HttpStatusCode.OK)
      },
    )
  }

  override suspend fun _shutdown() {

  }
}