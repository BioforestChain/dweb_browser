package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.MicroModule
import info.bagen.rust.plaoc.microService.helper.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import okhttp3.internal.notifyAll
import okhttp3.internal.wait

class NativeIpc(
    val port: NativePort<IpcMessage, IpcMessage>,
    override val remote: MicroModule,
    override val role: IPC_ROLE,
) : Ipc() {
    override suspend fun _doPostMessage(data: IpcMessage) {
        port.postMessage(data)
    }

    override suspend fun _doClose() {
        port.close()
    }
}

class NativePort<I, O>(
    private val channel_in: Channel<I>,
    private val channel_out: Channel<O>,
    private val closeMutex: Mutex
) {
    private var started = false
    fun start() {
        if (started || closing) return else started = true

        runBlocking {
            while (!channel_in.isClosedForReceive) {
                _messageSignal.emit(channel_in.receive())
            }
        }
    }


    private val _closeSignal = SimpleSignal()

    fun onClose(cb: SimpleCallback) = _closeSignal.listen(cb)

    private var closing = false
    fun close() {
        if (closing) return else closing = true
        closeMutex.notifyAll()
    }

    /**
     * 等待 close 信号被发出，那么就关闭出口、触发事件
     */
    init {
        runBlocking {
            closeMutex.wait()

            closing = true
            channel_out.close()
            _closeSignal.emit()
        }
    }


    private val _messageSignal = Signal<I>()

    /**
     * 发送消息，这个默认会阻塞
     */
    suspend fun postMessage(msg: O) {
        channel_out.send(msg)
    }

    /**
     * 监听消息
     */
    fun onMessage(cb: Callback<I>) = _messageSignal.listen(cb)
}

class NativeMessageChannel<T1, T2> {
    private val closeMutex = Mutex()
    private val channel1 = Channel<T1>()
    private val channel2 = Channel<T2>()
    val port1 = NativePort(channel1, channel2, closeMutex)
    val port2 = NativePort(channel2, channel1, closeMutex)
}