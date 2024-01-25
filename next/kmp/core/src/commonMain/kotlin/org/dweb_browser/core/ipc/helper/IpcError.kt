package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

@Serializable
data class IpcError(
  val errorCode: Int,
  val message: String? = null,
) : IpcMessage(IPC_MESSAGE_TYPE.ERROR) {
  override fun toString() = "IpcError(errorCode=$errorCode,message=$message)"
}