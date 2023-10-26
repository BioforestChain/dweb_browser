package org.dweb_browser.shared.microService.sys.notification

import kotlinx.serialization.Serializable


expect class NotifyManager() {

    fun createNotification(
        title: String,
        subTitle: String?,
        text: String,
        androidIcon: Int?,
        iosIcon: String?,
        bigText: String?,
        intentUrl: String?,
        commonChannelType: CommonChannelType,
        intentType: PendingIntentType
    )
}

enum class PendingIntentType {
    DEFAULT,BROADCAST
}

enum class CommonChannelType {
    DEFAULT,IMPORTANT
}


enum class MessageSource(value: String) {
    APP("app_message"), PUSH("push_message")
}

@Serializable
data class NotificationMsgItem(
    val app_id: String = "",
    val title: String = "",
    val msg_content: String = "",
    val msg_src: String = "app_message",
    val msg_priority: Int = 1,
    val entry_queue_time: String = "",
    val msg_status: String = "",
    val msg_id: String = "",
)