package info.bagen.kmmsharedmodule.ipc.ipcWeb

import info.bagen.kmmsharedmodule.ipc.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun jsonToIpcMessage(data: String, ipc: Ipc): Any? {
    if (data == "close" || data == "ping" || data == "pong") {
        return data
    }

    return runCatching {
        when (Json.decodeFromString<IpcMessage>(data).type) {
            IPC_MESSAGE_TYPE.REQUEST -> Json.decodeFromString<IpcReqMessage>(data).let {
                IpcRequest(
                    it.req_id,
                    it.url,
                    it.method,
                    IpcHeaders(it.headers),
                    IpcBodyReceiver.from(it.metaBody, ipc),
                    ipc
                )
            }
            IPC_MESSAGE_TYPE.RESPONSE -> Json.decodeFromString<IpcResMessage>(data).let {
                IpcResponse(
                    it.req_id,
                    it.statusCode,
                    IpcHeaders(it.headers),
                    IpcBodyReceiver.from(it.metaBody, ipc),
                    ipc
                )
            }
            IPC_MESSAGE_TYPE.EVENT -> Json.decodeFromString<IpcEvent>(data)
            IPC_MESSAGE_TYPE.STREAM_DATA -> Json.decodeFromString<IpcStreamData>(data)
            IPC_MESSAGE_TYPE.STREAM_PULL -> Json.decodeFromString<IpcStreamPull>(data)
            IPC_MESSAGE_TYPE.STREAM_END -> Json.decodeFromString<IpcStreamEnd>(data)
            IPC_MESSAGE_TYPE.STREAM_ABORT -> Json.decodeFromString<IpcStreamAbort>(data)
        }
    }.onFailure { e ->
        e.printStackTrace()
    }.getOrDefault(data)

}
