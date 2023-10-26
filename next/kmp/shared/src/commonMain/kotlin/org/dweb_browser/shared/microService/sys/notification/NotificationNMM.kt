package org.dweb_browser.shared.microService.sys.notification

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule

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
                    "app_message" -> CommonChannelType.DEFAULT
                    "push_message" -> CommonChannelType.IMPORTANT
                    else -> CommonChannelType.DEFAULT
                }
                notifyManager.createNotification(
                    title = message.title,
                    subTitle = "",
                    text = message.msg_content,
                    androidIcon = 0,
                    iosIcon = "",
                    bigText = message.msg_content,
                    intentUrl = "",
                    commonChannelType = channelType,
                    intentType = PendingIntentType.DEFAULT
                )
                PureResponse(HttpStatusCode.OK)
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}