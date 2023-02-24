package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.toBase64
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.*

class IpcBodySender(
    override val body: Any,
    ipc: Ipc,
) : IpcBody() {
    override val metaBody = bodyAsRawData(body, ipc)
    override val bodyHub by lazy {
        BodyHub().also {
            it.data = body
            when (body) {
                is String -> it.text = body;
                is ByteArray -> it.u8a = body
                is InputStream -> it.stream = body
            }
        }
    }

    companion object {

        private val streamIdWM by lazy { WeakHashMap<InputStream, String>() }

        private var stream_id_acc = 0;
        fun getStreamId(stream: InputStream): String = streamIdWM.getOrPut(stream) {
            "rs-${stream_id_acc++}"
        };

        private fun bodyAsRawData(body: Any, ipc: Ipc) = when (body) {
            is String -> textAsRawData(body, ipc)
            is ByteArray -> binaryAsRawData(body, ipc)
            is InputStream -> streamAsRawData(body, ipc)
            else -> throw Exception("invalid body type $body")
        }

        private fun textAsRawData(text: String, ipc: Ipc) = MetaBody(IPC_RAW_BODY_TYPE.TEXT, text)

        private fun binaryAsRawData(binary: ByteArray, ipc: Ipc) = if (ipc.supportBinary) {
            MetaBody(IPC_RAW_BODY_TYPE.BINARY, binary)
        } else {
            MetaBody(IPC_RAW_BODY_TYPE.BASE64, binary.toBase64())
        }

        private fun streamAsRawData(
            stream: InputStream, ipc: Ipc
        ): MetaBody {
            val stream_id = getStreamId(stream)
            debugStream("streamAsRawData/$ipc/$stream", stream_id)
            val streamAsRawDataScope =
                CoroutineScope(CoroutineName("streamAsRawData/$ipc/$stream/$stream_id") + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
                    e.printStackTrace()
                })
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
            return when (ipc.supportBinary) {
                true -> MetaBody(IPC_RAW_BODY_TYPE.BINARY_STREAM_ID, stream_id)
                false -> MetaBody(IPC_RAW_BODY_TYPE.BASE64_STREAM_ID, stream_id)
            }
        }


    }
}
