package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(IPC_MESSAGE_TYPE_STREAM_END)
class IpcStreamEnd(
  override val stream_id: String,
) : IpcRawMessage, IpcMessage, IpcStream