package org.dweb_browser.sys.notification

import kotlinx.serialization.Serializable
import org.dweb_browser.core.module.MicroModule

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


expect class NotificationManager() {
  suspend fun createNotification(microModule: MicroModule, message: NotificationMsgItem)
//  fun updateNotification()
//  fun cancelNotification()
}