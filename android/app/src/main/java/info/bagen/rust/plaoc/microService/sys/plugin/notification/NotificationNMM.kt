package info.bagen.rust.plaoc.microService.sys.plugin.notification

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
import org.http4k.lens.Query
import org.http4k.routing.bind
import org.http4k.routing.routes

class NotificationNMM : NativeMicroModule("notification.sys.dweb") {
    private val notifyManager = NotifyManager()
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext)
 {
        apiRouting = routes(
            /** 创建消息*/
            "/create" bind Method.GET to defineHandler { request ->
                val message = Query.auto<NotificationMsgItem>().required("msg")(request)
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