package info.bagen.rust.plaoc.microService.ipc

data class IpcStreamAbort(val stream_id: String) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_ABORT)