package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.toBase64

data class IpcStreamData(val stream_id: String, val data: Any /*String or ByteArray*/) :
    IpcMessage(IPC_DATA_TYPE.STREAM_PULL) {

    companion object {
        fun fromBinary(ipc: Ipc, stream_id: String, data: ByteArray) = if (ipc.supportBinary) {
            IpcStreamData(stream_id, data)
        } else {
            IpcStreamData(stream_id, data.toBase64())
        }
    }
}