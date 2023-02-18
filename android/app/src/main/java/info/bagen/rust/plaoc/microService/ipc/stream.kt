package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.SIGNAL_CTOR
import info.bagen.rust.plaoc.microService.helper.asBase64
import info.bagen.rust.plaoc.microService.helper.asUtf8
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStream

fun streamAsRawData(
    stream_id: String,
    stream: InputStream,
    ipc: Ipc
) {
    val channel = stream.toByteReadChannel()

    ipc.onMessage { args ->
        /// 对方申请数据拉取
        if (args.message is IpcStreamPull && args.message.stream_id == stream_id) {
            runBlocking {
                channel.read(args.message.desiredSize) {
                    val binary = it.array()
                    runBlocking {
                        ipc.postMessage(IpcStreamData.fromBinary(ipc, stream_id, binary))
                    }
                }
            }
        } else if (args.message is IpcStreamAbort && args.message.stream_id == stream_id) {
            withContext(Dispatchers.IO) {
                stream.close()
            }
        }
    }

}

/**
 * @return {String | ByteArray | InputStream}
 */
fun rawDataToBody(rawBody: RawData, ipc: Ipc): Any {
    val bodyEncoder =
        /// 二进制模式，直接返回即可
        if (rawBody.type and IPC_RAW_BODY_TYPE.BINARY !== 0) {
            { data: Any -> data as ByteArray }
        } else if (rawBody.type and IPC_RAW_BODY_TYPE.BASE64 !== 0) {
            { data: Any -> (data as String).asBase64() }
        } else if (rawBody.type and IPC_RAW_BODY_TYPE.TEXT !== 0) {
            { data: Any -> (data as String).asUtf8() }
        } else throw Exception("invalid rawBody.type :${rawBody.type}")

    if (rawBody.type and IPC_RAW_BODY_TYPE.STREAM_ID !== 0) {
        val stream_id = rawBody.data as String;
        val stream = ByteChannel();
        ipc.onMessage { args ->
            if (args.message is IpcStreamData && args.message.stream_id == stream_id) {
                runBlocking {
                    stream.write {
                        it.put(bodyEncoder(args.message.data))
                    }
                }
            } else if (args.message is IpcStreamEnd && args.message.stream_id == stream_id) {
                stream.close()
                return@onMessage SIGNAL_CTOR.OFF
            } else {
            }
        }
        return stream.toInputStream()
    }
    /// 文本模式，直接返回即可，因为 RequestInit/Response 支持支持传入 utf8 字符串
    return if (rawBody.type and IPC_RAW_BODY_TYPE.TEXT !== 0) {
        return rawBody.data as String
    } else bodyEncoder(rawBody.data)

}