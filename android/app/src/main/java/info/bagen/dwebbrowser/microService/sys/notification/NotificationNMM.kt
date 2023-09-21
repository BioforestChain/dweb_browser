package info.bagen.dwebbrowser.microService.sys.notification

import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.composite
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class NotificationNMM : NativeMicroModule("notification.sys.dweb", "notification") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);
  }

  private val notifyManager = NotifyManager()
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      /** 创建消息*/
      "/create" bind Method.GET to defineHandler { request ->

        val message = Query.composite {
          NotificationMsgItem(
            app_id = string().defaulted("app_id", "")(it),
            title = string().defaulted("title", "")(it),
            msg_content = string().defaulted("msg_content", "")(it),
            msg_src = string().defaulted("msg_src", "app_message")(it),
            msg_priority = int().defaulted("msg_priority", 1)(it),
            entry_queue_time = string().defaulted("entry_queue_time", "")(it),
            msg_status = string().defaulted("msg_status", "")(it),
            msg_id = string().defaulted("msg_id", "")(it),
          )
        }(request)
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
        Response(Status.OK)
      },
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}