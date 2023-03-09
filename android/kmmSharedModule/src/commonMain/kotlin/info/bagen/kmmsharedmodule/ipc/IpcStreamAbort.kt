package info.bagen.kmmsharedmodule.ipc

data class IpcStreamAbort(val stream_id: String) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_ABORT)