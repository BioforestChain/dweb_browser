package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import info.bagen.rust.plaoc.microService.MicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcMessage
import info.bagen.rust.plaoc.microService.ipc.IpcMessageArgs
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
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
    /** MessagePort 默认支持二进制传输 */
    override val supportMessagePack: Boolean = false
) : Ipc() {
    val context = this
    var _incomne_stream: ByteReadChannel? = null

    /**
     * 输入流要额外绑定
     * 注意，非必要不要 await 这个promise
     */
    fun bindIncomeStream(stream: InputStream) {
        if (this._incomne_stream !== null) {
            throw Error("in come stream already binded.");
        }
        _incomne_stream = ByteReadChannel(stream.readBytes()).also { _incomne_stream ->
            runBlocking {
                // 如果通道关闭并且没有剩余字节可供读取，则返回 true
                while (!_incomne_stream.isClosedForRead) {
                    val size = _incomne_stream.readInt() // 读满一个Int
                    // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
                    val chunk = _incomne_stream.readPacket(size)
                    println("ReadableStreamIpc#bindIncomeStream ==> ${chunk.readBytes()}") // 准确读取 n 个字节（如果未指定 n，则消耗所有剩余字节
                    val message =
                        messageToIpcMessage(chunk.readBytes(), context) ?: return@runBlocking
                    if (message === "close") {
                        context.close();
                        return@runBlocking
                    }
                    context._messageSignal.emit(message as IpcMessageArgs)
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
            context._incomne_stream?.readFully(message)
        }
    }

    override fun _doClose() {
        //  丢弃通道中的所有字节并暂停直到流结束。
       runBlocking {  context._incomne_stream?.discard() }
    }
}