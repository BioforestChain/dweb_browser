package org.dweb_browser.browser.desk.model

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict

data class ActivityItem(
  val id: String,
  val owner: MicroModule.Runtime,
  val icon: Icon,
  val content: Content,
  val action: Action,
) {
  @Serializable
  sealed interface Icon

  @Serializable
  @SerialName("none")
  data object NoneIcon : Icon

  @Serializable
  @SerialName("image")
  class ImageIcon(val url: String) : Icon

  @Serializable
  sealed interface Content

  @Serializable
  @SerialName("text")
  class TextContent(val shortText: String, val fullText: String) : Content

  @Serializable
  sealed interface Action

  @Serializable
  @SerialName("none")
  data object NoneAction : Action

  @Serializable
  @SerialName("cancel")
  class CancelAction(val text: String, val uri: String?) : Action

  @Serializable
  @SerialName("confirm")
  class ConfirmAction(val text: String, val uri: String?) : Action

  @Serializable
  @SerialName("link")
  class LinkAction(val text: String, val uri: String) : Action


  @Transient
  val renderProp = ActivityItemRenderProp()

  val appIcon by lazy { owner.icons.toStrict().pickLargest() }
}

class ActivityItemRenderProp {
  var open by mutableStateOf(true)
  val viewAni = Animatable(0f)
  var showDetail by mutableStateOf(false)
  val detailAni = Animatable(0f)
}