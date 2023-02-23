package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.ipc.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.InputStream


/**
 * 基于 WebReadableStream 的IPC
 *
 * 它会默认构建出一个输出流，
 * 以及需要手动绑定输入流 {@link bindIncomeStream}
 */
class ReadableStreamIpc(
    override val remote: MicroModule,
    override val role: IPC_ROLE,
) : Ipc() {
    private lateinit var controller: ReadableStream.ReadableStreamController

    val stream = ReadableStream(onStart = {
        controller = it
    }, onPull = { (size) ->
        debugStream("IPC-ON-PULL", size)
    })

    private var _incomeStream: InputStream? = null

    /**
     * 输入流要额外绑定
     * 注意，非必要不要 await 这个promise
     */
    fun bindIncomeStream(stream: InputStream, coroutineName: String) {
        if (this._incomeStream !== null) {
            throw Exception("in come stream already binded.");
        }
        if (supportMessagePack) {
            throw Exception("还未实现 MessagePack 的编解码能力")
        }

        _incomeStream = stream
        CoroutineScope(CoroutineName(coroutineName)).launch {
            // 如果通道关闭并且没有剩余字节可供读取，则返回 true
            while (stream.available() > 0) {
                val size = stream.readInt()
                // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
                val chunk = stream.readByteArray(size)
                when (val message =
                    jsonToIpcMessage(chunk.toString(Charsets.UTF_8), this@ReadableStreamIpc)) {
                    "close" -> close()
                    is IpcMessage -> _messageSignal.emit(
                        IpcMessageArgs(
                            message,
                            this@ReadableStreamIpc
                        )
                    )
                    else -> throw Exception("unknown message: $message")
                }
            }
        }
    }

    override suspend fun _doPostMessage(data: IpcMessage) {
        val message = when {
            supportMessagePack -> moshiPack.packToByteArray(data)
            else -> when (data) {
                is IpcRequest -> gson.toJson(data.ipcReqMessage).asUtf8()
                is IpcResponse -> gson.toJson(data.ipcResMessage).asUtf8()
                else -> gson.toJson(data).asUtf8()
            }
        }
        controller.enqueue(message.size.toByteArray() + message)
    }

    override suspend fun _doClose() {
        controller.close()
    }
}