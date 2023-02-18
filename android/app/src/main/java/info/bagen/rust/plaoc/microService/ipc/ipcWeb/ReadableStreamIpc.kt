package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import info.bagen.rust.plaoc.microService.MicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcMessage
import info.bagen.rust.plaoc.microService.ipc.IpcMessageArgs
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
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
    val context = this
    private var _incomeStream: ByteReadChannel? = null

    /**
     * 输入流要额外绑定
     * 注意，非必要不要 await 这个promise
     */
    fun bindIncomeStream(stream: InputStream) {
        if (this._incomeStream !== null) {
            throw Exception("in come stream already binded.");
        }

        _incomeStream = stream.toByteReadChannel().also { _income_stream ->
            runBlocking {
                // 如果通道关闭并且没有剩余字节可供读取，则返回 true
                while (!_income_stream.isClosedForRead) {
                    val size = _income_stream.readInt() // 读满一个Int
                    // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
                    val chunk = _income_stream.readPacket(size)
                    println("ReadableStreamIpc#bindIncomeStream ==> ${chunk.readBytes()}") // 准确读取 n 个字节（如果未指定 n，则消耗所有剩余字节
                    when (val message = messageToIpcMessage(chunk.readText(), context)) {
                        "close" -> context.close()
                        is IpcMessage -> context._messageSignal.emit(
                            IpcMessageArgs(
                                message,
                                this@ReadableStreamIpc
                            )
                        )
                    }
                }
            }
        }
    }

    override suspend fun _doPostMessage(data: IpcMessage) {
        val message = if (this.supportMessagePack) {
            println("ReadableStreamIpc#_doPostMessage===>${moshiPack.pack(data)}")
            moshiPack.packToByteArray(data)
        } else {
            println("ReadableStreamIpc#message===>$data")
            data.toString().asBase64() // TODO data.toString?
        }
        runBlocking {
            context._incomeStream?.readFully(message)
        }
    }

    override suspend fun _doClose() {
        //  丢弃通道中的所有字节并暂停直到流结束。
        context._incomeStream?.discard()
        context._incomeStream = null
    }
}