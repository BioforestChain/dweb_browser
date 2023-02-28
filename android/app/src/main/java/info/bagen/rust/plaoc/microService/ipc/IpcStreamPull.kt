package info.bagen.rust.plaoc.microService.ipc

data class IpcStreamPull(val stream_id: String, val desiredSize: Int = 1) :
    IpcMessage(IPC_MESSAGE_TYPE.STREAM_PULL)