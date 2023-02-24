package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.SIGNAL_CTOR
import info.bagen.rust.plaoc.microService.helper.asBase64
import info.bagen.rust.plaoc.microService.helper.asUtf8
import java.io.InputStream


class IpcBodyReceiver(
    override val metaBody: MetaBody,
    private val ipc: Ipc
) : IpcBody() {

    /// 因为是 abstract，所以得用 lazy 来延迟得到这些属性
    override val bodyHub by lazy {
        BodyHub().also {
            val data = rawDataToBody(metaBody, ipc)
            it.data = data
            when (data) {
                is String -> it.text = data;
                is ByteArray -> it.u8a = data
                is InputStream -> it.stream = data
            }
        }
    }

    companion object {


        /**
         * @return {String | ByteArray | InputStream}
         */
        fun rawDataToBody(metaBody: MetaBody?, ipc: Ipc?): Any {
            if (metaBody == null || ipc == null) {
                return ""
            }
            val bodyEncoder =
                /// 二进制模式，直接返回即可
                if (metaBody.type and IPC_RAW_BODY_TYPE.BINARY != 0) {
                    { data: Any -> data as ByteArray }
                } else if (metaBody.type and IPC_RAW_BODY_TYPE.BASE64 != 0) {
                    { data: Any -> (data as String).asBase64() }
                } else if (metaBody.type and IPC_RAW_BODY_TYPE.TEXT != 0) {
                    { data: Any -> (data as String).asUtf8() }
                } else throw Exception("invalid metaBody.type :${metaBody.type}")

            if (metaBody.type and IPC_RAW_BODY_TYPE.STREAM_ID != 0) {
                val stream_id = metaBody.data as String;
                val stream = ReadableStream(onStart = { controller ->
                    ipc.onMessage { (message) ->
                        debugStream("rawDataToBody/onMessage/$ipc/${controller.stream}", message)
                        if (message is IpcStreamData && message.stream_id == stream_id) {
                            controller.enqueue(bodyEncoder(message.data))
                        } else if (message is IpcStreamEnd && message.stream_id == stream_id) {
                            controller.close()
                            return@onMessage SIGNAL_CTOR.OFF
                        } else {
                        }
                    }
                }, onPull = { (desiredSize, controller) ->
                    debugStream(
                        "rawDataToBody/postPullMessage/$ipc/${controller.stream}",
                        stream_id
                    )
                    ipc.postMessage(IpcStreamPull(stream_id, desiredSize))
                });
                debugStream("rawDataToBody/$ipc/$stream", stream_id)

                return stream // as InputStream
            }
            /// 文本模式，直接返回即可，因为 RequestInit/Response 支持支持传入 utf8 字符串
            return if (metaBody.type and IPC_RAW_BODY_TYPE.TEXT != 0) {
                return metaBody.data as String
            } else bodyEncoder(metaBody.data)

        }
    }
}