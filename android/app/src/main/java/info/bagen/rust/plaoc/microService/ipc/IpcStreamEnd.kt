package info.bagen.rust.plaoc.microService.ipc

class IpcStreamEnd(val stream_id: String) : IpcMessage(IPC_DATA_TYPE.STREAM_END) {}