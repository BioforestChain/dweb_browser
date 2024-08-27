package org.dweb_browser.browser.desk.model

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict

class ActivityItem(
  val key: String = randomUUID(),
  val owner: MicroModule.Runtime,
  leadingIcon: Icon,
  trailingIcon: Icon,
  centerTitle: Content,
  bottomActions: List<Action> = emptyList(),
) {
  val id get() = "${owner.id}:$key"
  var leadingIcon by mutableStateOf(leadingIcon)
  var trailingIcon by mutableStateOf(trailingIcon)
  var centerTitle by mutableStateOf(centerTitle)
  var bottomActions by mutableStateOf(bottomActions)

  @Serializable
  sealed interface Icon

  @Serializable
  @SerialName("none")
  data object NoneIcon : Icon

  /**
   * 支持动态图片，比如 Gif/WebP
   */
  @Serializable
  @SerialName("image")
  class ImageIcon(val url: String) : Icon

  class ComposeIcon(val content: @Composable (Modifier) -> Unit) : Icon

  @Serializable
  sealed interface Content

  @Serializable
  @SerialName("text")
  class TextContent(val text: String) : Content

  @Serializable
  sealed interface Action

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
  val viewAniRunning get() = viewAni.isRunning || viewAni.value != 0f
  val viewAniFinished get() = !viewAni.isRunning && viewAni.value == 1f
  val detailAniRunning get() = detailAni.isRunning || detailAni.value != 0f
  val detailAniFinished get() = !detailAni.isRunning && detailAni.value == 1f
  val canView get() = open || viewAniRunning
  val canViewDetail get() = showDetail || detailAniRunning
}