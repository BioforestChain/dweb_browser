package info.bagen.rust.plaoc.microService.ipc

data class IpcStreamPull(val stream_id: String, val desiredSize: Int = 1) :
    IpcMessage {
    override val type = IPC_DATA_TYPE.STREAM_PULL
}