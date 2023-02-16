package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.ipc.helper.IPC_DATA_TYPE

data class IpcRequest(
    val req_id: Number = 0,
    val method: String = "",
    val url: String = "",
    val body: RawData,
    val headers: IpcHeaders = IpcHeaders()
) : IpcMessage(IPC_DATA_TYPE.REQUEST) {
}
