package info.bagen.rust.plaoc.microService.ipc

data class IpcStreamAbort(override val stream_id: String) :
    IpcMessage(IPC_MESSAGE_TYPE.STREAM_ABORT), IpcStream