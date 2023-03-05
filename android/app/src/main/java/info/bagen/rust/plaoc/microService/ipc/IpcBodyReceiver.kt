package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.SIGNAL_CTOR
import info.bagen.rust.plaoc.microService.helper.debugger
import java.io.InputStream


/**
 * metaBody 可能会被多次转发，
 * 但只有第一次得到这个 metaBody 的 ipc 才是它真正意义上的 Receiver
 */
class IpcBodyReceiver(
    override val metaBody: MetaBody,
    ipc: Ipc,
) : IpcBody() {

    /// 因为是 abstract，所以得用 lazy 来延迟得到这些属性
    override val bodyHub by lazy {
        BodyHub().also {
            val data = when (metaBody.type) {
                IPC_META_BODY_TYPE.STREAM_ID -> {
                    val ipc =
                        metaIdIpcMap[metaId] ?: throw Exception("no found ipc by metaId:$metaId")
                    metaToStream(metaBody, ipc)
                }
                /// 文本模式，直接返回即可，因为 RequestInit/Response 支持支持传入 utf8 字符串
                IPC_META_BODY_TYPE.TEXT -> metaBody.data as String
                else -> metaBody.binary
            }
            it.data = data
            when (data) {
                is String -> it.text = data;
                is ByteArray -> it.u8a = data
                is InputStream -> it.stream = data
            }
        }
    }

    private val metaId by lazy { "${metaBody.ipcUid}/${metaBody.data}" }

    init {
        /// 将第一次得到这个metaBody的 ipc 保存起来
        if (metaBody.type == IPC_META_BODY_TYPE.STREAM_ID) {
            metaIdIpcMap.getOrPut(metaId) {
                ipc.onClose {
                    metaIdIpcMap.remove(metaId)
                }
                ipc
            }
        }
    }

    companion object {

        private val metaIdIpcMap = mutableMapOf<String, Ipc>()

        /**
         * @return {String | ByteArray | InputStream}
         */
        fun metaToStream(metaBody: MetaBody, ipc: Ipc): InputStream {
            /// metaToStream
            val stream_id = metaBody.data as String;
            val stream = ReadableStream(
                cid = "receiver-${stream_id}",
                onStart = { controller ->
                    ipc.onMessage { (message) ->
                        if (message is IpcStreamData && message.stream_id == stream_id) {
                            debugIpcBody(
                                "receiver/StreamData/$ipc/${controller.stream}", message
                            )
                            if (stream_id == "rs-0") {
                                debugger()
                            }
                            controller.enqueue(message.binary)
                        } else if (message is IpcStreamEnd && message.stream_id == stream_id) {
                            debugIpcBody(
                                "receiver/StreamEnd/$ipc/${controller.stream}", message
                            )
                            controller.close()
                            return@onMessage SIGNAL_CTOR.OFF
                        } else {
                        }
                    }
                },
                onPull = { (desiredSize, controller) ->
                    debugIpcBody(
                        "receiver/postPullMessage/$ipc/${controller.stream}", stream_id
                    )
                    ipc.postMessage(IpcStreamPull(stream_id, 1))
                });
            debugIpcBody("receiver/$ipc/$stream", "start by stream-id:${stream_id}")

            return stream

        }
    }
}