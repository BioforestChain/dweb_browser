package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.SIGNAL_CTOR
import info.bagen.rust.plaoc.microService.helper.asBase64
import info.bagen.rust.plaoc.microService.helper.asUtf8
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream


fun streamAsRawData(
    stream_id: String, stream: InputStream, ipc: Ipc
) {
    debugStream("streamAsRawData/$ipc/$stream")
    val streamAsRawDataScope = CoroutineScope(CoroutineName("streamAsRawData/${ipc}/$stream") + Dispatchers.IO)
    ipc.onMessage { (message) ->
        /// 对方申请数据拉取
        if ((message is IpcStreamPull) && (message.stream_id == stream_id)) {
            streamAsRawDataScope.launch {
                var desiredSize = message.desiredSize
                while (desiredSize > 0) {
                    debugStream("streamAsRawData/ON-PULL/$ipc/$stream", stream_id)
                    debugStream("streamAsRawData/READING/$ipc/$stream", stream_id)
                    when (val availableLen = stream.available()) {
                        -1, 0 -> {
                            ipc.postMessage(IpcStreamEnd(stream_id))
                            break
                        }
                        else -> {
                            // TODO 这里可能要限制每次的传输数量吗，根据 message.desiredSize
                            debugStream(
                                "streamAsRawData/READ/$ipc/$stream",
                                "$availableLen >> $stream_id"
                            )
                            val binary = ByteArray(availableLen)
                            stream.read(binary)
                            ipc.postMessage(
                                IpcStreamData.fromBinary(
                                    ipc, stream_id, binary
                                )
                            )
                            desiredSize -= availableLen
                        }
                    }
                }
                debugStream("streamAsRawData/END$ipc/$stream", stream_id)
            }
        } else if ((message is IpcStreamAbort) && (message.stream_id == stream_id)) {
            stream.close()
        } else {
        }
    }
}

/**
 * @return {String | ByteArray | InputStream}
 */
fun rawDataToBody(rawBody: RawData?, ipc: Ipc?): Any {
    if (rawBody == null || ipc == null) {
        return ""
    }
    val bodyEncoder =
        /// 二进制模式，直接返回即可
        if (rawBody.type and IPC_RAW_BODY_TYPE.BINARY != 0) {
            { data: Any -> data as ByteArray }
        } else if (rawBody.type and IPC_RAW_BODY_TYPE.BASE64 != 0) {
            { data: Any -> (data as String).asBase64() }
        } else if (rawBody.type and IPC_RAW_BODY_TYPE.TEXT != 0) {
            { data: Any -> (data as String).asUtf8() }
        } else throw Exception("invalid rawBody.type :${rawBody.type}")

    if (rawBody.type and IPC_RAW_BODY_TYPE.STREAM_ID != 0) {
        val stream_id = rawBody.data as String;
        val stream = ReadableStream(onStart = { controller ->
            ipc.onMessage { (message) ->
                debugStream("rawDataToBody/onMessage", message)
                if (message is IpcStreamData && message.stream_id == stream_id) {
                    controller.enqueue(bodyEncoder(message.data))
                } else if (message is IpcStreamEnd && message.stream_id == stream_id) {
                    controller.close()
                    return@onMessage SIGNAL_CTOR.OFF
                } else {
                }
            }
        }, onPull = { (desiredSize, controller) ->
            debugStream("rawDataToBody/POST-PULL/${controller.stream}", stream_id)
            ipc.postMessage(IpcStreamPull(stream_id, desiredSize))
        });
        debugStream("rawDataToBody/$stream")

        return stream // as InputStream
    }
    /// 文本模式，直接返回即可，因为 RequestInit/Response 支持支持传入 utf8 字符串
    return if (rawBody.type and IPC_RAW_BODY_TYPE.TEXT != 0) {
        return rawBody.data as String
    } else bodyEncoder(rawBody.data)

}