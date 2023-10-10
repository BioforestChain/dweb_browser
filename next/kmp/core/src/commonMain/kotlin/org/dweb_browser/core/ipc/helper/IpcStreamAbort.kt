package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

@Serializable
data class IpcStreamAbort(override val stream_id: String) :
  IpcMessage(IPC_MESSAGE_TYPE.STREAM_ABORT), IpcStream