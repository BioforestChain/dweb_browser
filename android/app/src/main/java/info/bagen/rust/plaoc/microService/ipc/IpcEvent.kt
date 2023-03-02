package info.bagen.rust.plaoc.microService.ipc

class IpcEvent(val name: String, val data: Any) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_EVENT)