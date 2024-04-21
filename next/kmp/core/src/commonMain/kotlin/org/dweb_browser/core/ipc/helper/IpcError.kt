package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(IPC_MESSAGE_TYPE_ERROR)
data class IpcError(
  val errorCode: Int,
  val message: String? = null,
) : IpcRawMessage, IpcMessage {
  override fun toString() = "IpcError(errorCode=$errorCode,message=$message)"
}