package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.dweb_browser.core.help.types.CommonAppManifest
import org.dweb_browser.helper.OrderBy

/**
 * Ipc生命周期控制
 */
@Serializable
@SerialName(IPC_MESSAGE_TYPE_LIFECYCLE)
data class IpcLifecycle(
  @Polymorphic val state: IpcLifecycleState,
  override val order: Int = DEFAULT_ORDER,
) : IpcRawMessage, IpcMessage, OrderBy {
  companion object {
    const val DEFAULT_ORDER = -1
  }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Polymorphic
@JsonClassDiscriminator("name")
sealed interface IpcLifecycleState

const val IPC_LIFECYCLE_STATE_INIT = "init"
const val IPC_LIFECYCLE_STATE_OPENING = "opening"
const val IPC_LIFECYCLE_STATE_OPENED = "opened"
const val IPC_LIFECYCLE_STATE_CLOSING = "closing"
const val IPC_LIFECYCLE_STATE_CLOSED = "closed"

@Serializable
@SerialName(IPC_LIFECYCLE_STATE_INIT)
data class IpcLifecycleInit(
  val pid: Int,
  val locale: CommonAppManifest,
  val remote: CommonAppManifest,
) : IpcLifecycleState

@Serializable
@SerialName(IPC_LIFECYCLE_STATE_OPENING)
data object IpcLifecycleOpening : IpcLifecycleState

@Serializable
@SerialName(IPC_LIFECYCLE_STATE_OPENED)
data object IpcLifecycleOpened : IpcLifecycleState

@Serializable
@SerialName(IPC_LIFECYCLE_STATE_CLOSING)
data class IpcLifecycleClosing(val reason: String? = null) : IpcLifecycleState

@Serializable
@SerialName(IPC_LIFECYCLE_STATE_CLOSED)
data class IpcLifecycleClosed(val reason: String? = null) : IpcLifecycleState

