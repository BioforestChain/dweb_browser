package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import info.bagen.rust.plaoc.microService.helper.moshiPack
import info.bagen.rust.plaoc.microService.ipc.*
import okio.BufferedSource

fun messageToIpcMessage(data: Any, ipc: Ipc): Any? {
    var data = data
    // 解码缓冲区中的单个 MessagePack 对象
    if (data is ByteArray || data is BufferedSource) {
        data = moshiPack.unpack(data as ByteArray) as IpcMessage
    }

    if (data === "close") {
        return data
    }
    val message = data as IpcMessage
    return when (data.type) {
        IPC_DATA_TYPE.REQUEST -> {
            val req = message as IpcRequest
            IpcRequest(req.req_id, req.method, req.url,  req.headers ,req.rawBody, ipc)
        }
        IPC_DATA_TYPE.RESPONSE -> {
            val res = message as IpcResponse
            IpcResponse(res.req_id, res.statusCode, res.headers, res.rawBody, ipc)
        }
        IPC_DATA_TYPE.STREAM_DATA -> {
            val streamData = message as IpcStreamData
            IpcStreamData(streamData.stream_id, streamData.data)
        }
        IPC_DATA_TYPE.STREAM_PULL -> {
            val streamData = message as IpcStreamPull
            IpcStreamPull(streamData.stream_id, streamData.desiredSize)
        }
        IPC_DATA_TYPE.STREAM_END -> {
            val streamData = message as IpcStreamEnd
            IpcStreamEnd(streamData.stream_id)
        }
        else -> {
            message
        }
    }
}

