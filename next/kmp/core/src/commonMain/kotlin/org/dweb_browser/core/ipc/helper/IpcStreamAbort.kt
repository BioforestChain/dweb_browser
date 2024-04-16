package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(IPC_MESSAGE_TYPE_STREAM_ABORT)
data class IpcStreamAbort(override val stream_id: String) :
  IpcMessage, IpcStream