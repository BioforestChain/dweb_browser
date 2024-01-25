package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

@Serializable
class IpcStreamEnd(
  override val stream_id: String
) :
  IpcMessage(IPC_MESSAGE_TYPE.STREAM_END),
  IpcStream