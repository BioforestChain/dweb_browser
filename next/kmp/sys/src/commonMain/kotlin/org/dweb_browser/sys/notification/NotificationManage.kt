package org.dweb_browser.sys.notification

import kotlinx.serialization.Serializable
import org.dweb_browser.core.module.MicroModule

@Serializable
data class NotificationWebItem(
  val dir: String = "auto", // 显示通知的方向。默认 auto, 另外两个值是 ltr 和 rtl
  val lang: String = "", // 通知的语言，根据 RFC 5646：用于识别语言的标记（也称为 BCP 47）使用表示语言标记的字符串指定。默认值为空字符串。
  val badge: String = "", // 一个字符串，其中包含图像的 URL，用于在没有足够的空间显示通知本身时表示通知。
  val body: String = "", // 一个字符串，表示通知的正文文本，显示在标题下方。默认值为空字符串。
  val icon: String = "", // 包含要在通知中显示的图标的 URL 的字符串。
  val image: String = "", // 一个字符串，其中包含要在通知中显示的图像的 URL。
  val data: String? = null, // 要与通知关联的任意数据。这可以是任何数据类型。默认值为 null 。
  val silent: Boolean? = null, // 一个布尔值，用于指定通知是否无提示（不发出声音或振动），而不考虑设备设置。默认值为 null 。如果 true ，则 vibrate 必须不存在。
  val vibrate: List<Long>? = null, // 设备的振动硬件随通知发出的振动模式。如果指定， silent 则不得为 true 。
  val timestamp: Long = 0L, // 一个数字，表示创建通知或适用通知的时间（过去、现在或将来）。
  val tag: String = "", // 一个字符串，表示通知的标识标记。默认值为空字符串。
  val renotify: Boolean = false, // 一个布尔值，指定在新通知替换旧通知后是否应通知用户。默认值为 false ，这意味着他们不会收到通知。如果 true ，则 tag 也必须设置。
  val requireInteraction: Boolean = false, // 指示通知应保持活动状态，直到用户单击或关闭通知，而不是自动关闭。默认值为 false 。
  val actions: List<Action> = emptyList(), // 要在通知中显示的操作数组，其默认值为空数组
) {
  @Serializable
  data class Action(
    val action: String, // 一个字符串，标识要在通知上显示的用户操作。
    val title: String, // 包含要向用户显示的操作文本的字符串。
    val icon: String, // 一个字符串，其中包含要随操作一起显示的图标的 URL。
  )
}

expect class NotificationManager() {
  suspend fun createNotification(microModule: MicroModule.Runtime, message: NotificationWebItem)
//  fun updateNotification()
//  fun cancelNotification()
}