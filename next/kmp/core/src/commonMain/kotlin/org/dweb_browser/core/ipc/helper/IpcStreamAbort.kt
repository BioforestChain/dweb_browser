package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.OrderBy

@Serializable
@SerialName(IPC_MESSAGE_TYPE_STREAM_ABORT)
data class IpcStreamAbort(
  override val stream_id: String,
  override val order: Int = stream_id.hashCode(),
) :
  IpcRawMessage, IpcMessage, IpcStream, OrderBy