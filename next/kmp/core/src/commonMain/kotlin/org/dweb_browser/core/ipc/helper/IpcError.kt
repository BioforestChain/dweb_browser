package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

@Serializable
data class IpcError(
  val channelId: Int,
  val errorCode: Int,
  val message: String = "",
) : IpcMessage(IPC_MESSAGE_TYPE.ERROR) {
  override fun toString() = "IpcError(channelId=$channelId,errorCode=$errorCode,message=$message)"
}