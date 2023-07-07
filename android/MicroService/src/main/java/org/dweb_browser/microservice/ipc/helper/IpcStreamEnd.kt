package org.dweb_browser.microservice.ipc.helper

class IpcStreamEnd(override val stream_id: String) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_END),
  IpcStream