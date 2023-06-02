package info.bagen.dwebbrowser.microService.core.ipc

class IpcStreamEnd(override val stream_id: String) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_END),
  IpcStream