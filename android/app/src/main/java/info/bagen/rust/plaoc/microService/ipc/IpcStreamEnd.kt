package info.bagen.rust.plaoc.microService.ipc

class IpcStreamEnd(val stream_id: String) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_END) {}