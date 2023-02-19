package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.ipc.*

fun jsonToIpcMessage(data: String, ipc: Ipc): Any? {
    if (data == "close") {
        return data
    }

    return runCatching {
        when (gson.fromJson(data, IpcMessage::class.java).type) {
            IPC_DATA_TYPE.REQUEST -> gson.fromJson(data, IpcRequestData::class.java).let {
                IpcRequest(it.req_id, it.ipcMethod, it.url, it.headers, it.rawBody, ipc)
            }
            IPC_DATA_TYPE.RESPONSE -> gson.fromJson(data, IpcResponseData::class.java).let {
                IpcResponse(it.req_id, it.statusCode, it.headers, it.rawBody, ipc)
            }
            IPC_DATA_TYPE.STREAM_DATA -> gson.fromJson(data, IpcStreamData::class.java)
            IPC_DATA_TYPE.STREAM_PULL -> gson.fromJson(data, IpcStreamPull::class.java)
            IPC_DATA_TYPE.STREAM_END -> gson.fromJson(data, IpcStreamEnd::class.java)
            IPC_DATA_TYPE.STREAM_ABORT -> gson.fromJson(data, IpcStreamAbort::class.java)
        }
    }.getOrDefault(data)

}
