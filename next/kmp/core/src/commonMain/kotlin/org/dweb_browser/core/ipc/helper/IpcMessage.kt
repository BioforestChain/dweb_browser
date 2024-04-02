package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

/**
 * 总的消息类型抽象
 */
@Serializable
sealed class IpcMessage(val type: IPC_MESSAGE_TYPE)

sealed interface IpcStream {
  val stream_id: String
}

