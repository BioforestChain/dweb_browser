package org.dweb_browser.microservice.ipc

import com.sun.tools.javac.Main
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.helper.readInt
import org.dweb_browser.helper.toByteArray
import org.dweb_browser.helper.toUtf8ByteArray
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.help.moshiPack
import org.dweb_browser.microservice.ipc.message.jsonToIpcMessage
import org.dweb_browser.microservice.ipc.message.IPC_ROLE
import org.dweb_browser.microservice.ipc.message.IpcMessage
import org.dweb_browser.microservice.ipc.message.IpcMessageArgs
import org.dweb_browser.microservice.ipc.message.IpcRequest
import org.dweb_browser.microservice.ipc.message.IpcResponse
import org.dweb_browser.microservice.ipc.message.IpcStreamData
import org.dweb_browser.microservice.ipc.message.ReadableStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference


fun debugStreamIpc(tag: String, msg: Any = "", err: Throwable? = null) =
  printdebugln("stream-ipc", tag, msg, err)

/**
 * 基于 WebReadableStream 的IPC
 *
 * 它会默认构建出一个输出流，
 * 以及需要手动绑定输入流 {@link bindIncomeStream}
 */
class ReadableStreamIpc(
  override val remote: MicroModuleInfo,
  override val role: String,
) : Ipc() {
    companion object {
        val incomeStreamCoroutineScope =
            CoroutineScope(CoroutineName("income-stream") + ioAsyncExceptionHandler)
    }

    constructor(
        remote: MicroModuleInfo,
        role: IPC_ROLE,
    ) : this(remote, role.role)

    override fun toString(): String {
        return super.toString() + "@ReadableStreamIpc"
    }
    // 虽然 ReadableStreamIpc 支持 Binary 的传输，但是不支持结构化的传输，
    // override val supportBinary: Boolean = true

    private lateinit var controller: ReadableStream.ReadableStreamController

    val stream = ReadableStream(cid = role, onStart = {
        controller = it
    }, onPull = { (size, controller) ->
        debugStreamIpc("ON-PULL/${controller.stream}", size)
    }, onClose = {
        inComeStreamJob.getAndSet(null)?.cancel()
    })
    private var inComeStreamJob = AtomicReference<Job?>(null)

    private suspend fun enqueue(data: ByteArray) = controller.enqueue(data)

    private var _incomeStream: InputStream? = null

    private val PONG_DATA by lazy {
        val pong = "pong".toByteArray()
        pong.size.toByteArray() + pong
    }

    /**
     * 输入流要额外绑定
     */
    @Synchronized
    fun bindIncomeStream(stream: InputStream, coroutineName: String = role) {
        if (this._incomeStream !== null) {
            throw Exception("in come stream already binded.");
        }
        if (supportMessagePack) {
            throw Exception("还未实现 MessagePack 的编解码能力")
        }
        //
//        val j = GlobalScope.launch {
//            while (true) {
//                delay(10000)
//                debugStreamIpc("LIVE/$stream")
//            }
//        }
        val readStream: suspend () -> Unit = {
            // 如果通道关闭并且没有剩余字节可供读取，则返回 true
            while (stream.available() > 0) {
                val size = stream.readInt()
                if (size <= 0) { // 心跳包？
                    continue
                }
                debugStreamIpc("size/$stream", size)
                // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
                val chunk = stream.readByteArray(size).toString(Charsets.UTF_8)

                val message = jsonToIpcMessage(chunk, this@ReadableStreamIpc)
                when (message) {
                    "close" -> close()
                    "ping" -> enqueue(PONG_DATA)
                    "pong" -> debugStreamIpc("PONG/$stream")
                    is IpcMessage -> {
                        debugStreamIpc("ON-MESSAGE/${this@ReadableStreamIpc}", message)
                        _messageSignal.emit(
                            IpcMessageArgs(
                                message, this@ReadableStreamIpc
                            )
                        )
                    }
                    else -> throw Exception("unknown message: $message")
                }
            }
            debugStreamIpc("END/$stream")
            // 流是双向的，对方关闭的时候，自己也要关闭掉
            this.close()
        }
        _incomeStream = stream
        inComeStreamJob.getAndSet(incomeStreamCoroutineScope.async { readStream() })
            ?.cancel() // 这里的cancel理论上不会触发
    }

    override suspend fun _doPostMessage(data: IpcMessage) {
        val message = when {
            supportMessagePack -> moshiPack.packToByteArray(data)
            else -> when (data) {
                is IpcRequest -> gson.toJson(data.ipcReqMessage).toUtf8ByteArray()
                is IpcResponse -> gson.toJson(data.ipcResMessage).toUtf8ByteArray()
                is IpcStreamData -> gson.toJson(data).toUtf8ByteArray()
                else -> gson.toJson(data).toUtf8ByteArray()
            }
        }
        debugStreamIpc("post/$stream", message.size)
        enqueue(message.size.toByteArray() + message)
    }


    override suspend fun _doClose() {
        controller.close()
        withContext(Dispatchers.IO) {
            _incomeStream?.close()
        }
    }
}
