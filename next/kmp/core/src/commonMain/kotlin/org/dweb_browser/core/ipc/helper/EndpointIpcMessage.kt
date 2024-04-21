package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**分发消息到各个ipc的监听时使用
 *
 * 这里的 orderBy 和 IpcMessage 里头的 orderBy 不冲突，是两层各自去排序
 */
@Serializable
@SerialName(ENDPOINT_MESSAGE_TYPE_IPC)
data class EndpointIpcRawMessage(
  val pid: Int,
  @Polymorphic val ipcMessage: IpcRawMessage,
) : EndpointRawMessage

data class EndpointIpcMessage(
  val pid: Int,
  @Polymorphic val ipcMessage: IpcMessage,
) : EndpointMessage, RawAble<EndpointIpcRawMessage> {
  override val stringAble by lazy {
    EndpointIpcRawMessage(pid, ipcMessage.toIpcRawMessage(false))
  }
  override val binaryAble by lazy {
    EndpointIpcRawMessage(pid, ipcMessage.toIpcRawMessage(true))
  }
}