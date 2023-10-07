package info.bagen.dwebbrowser.microService.sys.notification

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.bind

class NotificationNMM : NativeMicroModule("notification.sys.dweb", "notification") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);
  }

  private val notifyManager = NotifyManager()
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 创建消息*/
      "/create" bind HttpMethod.Get to definePureResponse {
        val message = request.queryAs<NotificationMsgItem>()
        val channelType = when (message.msg_src) {
          "app_message" -> NotifyManager.ChannelType.DEFAULT
          "push_message" -> NotifyManager.ChannelType.IMPORTANT
          else -> NotifyManager.ChannelType.DEFAULT
        }
        notifyManager.createNotification(
          title = message.title,
          text = message.msg_content,
          bigText = message.msg_content,
          channelType = channelType,
        )
        PureResponse(HttpStatusCode.OK)
      },
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}