package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.Callback
import info.bagen.rust.plaoc.microService.helper.Signal
import info.bagen.rust.plaoc.microService.helper.SimpleCallback
import info.bagen.rust.plaoc.microService.helper.SimpleSignal
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NativeIpc(
    val port: NativePort<IpcMessage, IpcMessage>,
    override val remote: MicroModule,
    override val role: IPC_ROLE,
) : Ipc() {
    init {
        port.onMessage { message ->
//            val ipcMessage = when (message.type) {
//                IPC_DATA_TYPE.REQUEST -> (message as IpcRequest).let { fromRequest ->
//                    /**
//                     * fromRequest 携带者远端的 ipc 对象，不是我们的 IpcRequest 对象。
//                     */
//                    IpcRequest.fromRequest(fromRequest.req_id, fromRequest.asRequest(), this)
//                }
//                IPC_DATA_TYPE.RESPONSE -> (message as IpcResponse).let { fromResponse ->
//                    /**
//                     * fromResponse 携带者远端的 ipc 对象，不是我们的 IpcResponse 对象。
//                     */
//                    IpcResponse.fromResponse(fromResponse.req_id, fromResponse.asResponse(), this)
//                }
//                /**
//                 * 其它情况的对象可以直接复用
//                 */
//                else -> message
//            }
//            _messageSignal.emit(IpcMessageArgs(ipcMessage, this))
            _messageSignal.emit(IpcMessageArgs(message, this))
            null
        }
        port.start()
    }

    override suspend fun _doPostMessage(data: IpcMessage) {
        port.postMessage(data)
    }

    override suspend fun _doClose() {
        port.close()
    }
}

@OptIn(DelicateCoroutinesApi::class)
class NativePort<I, O>(
    private val channel_in: Channel<I>,
    private val channel_out: Channel<O>,
    private val closeMutex: Mutex
) {
    private var started = false
    fun start() {
        if (started || closing) return else started = true

        GlobalScope.launch {
            for (message in channel_in) {
                _messageSignal.emit(message)
            }
        }
    }


    private val _closeSignal = SimpleSignal()

    fun onClose(cb: SimpleCallback) = _closeSignal.listen(cb)

    private var closing = false
    fun close() {
        if (closing) return else closing = true
        closeMutex.unlock()
    }

    /**
     * 等待 close 信号被发出，那么就关闭出口、触发事件
     */
    init {
        GlobalScope.launch {
            closeMutex.withLock {
                closing = true
                channel_out.close()
                _closeSignal.emit()
            }
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
    /**
     * 默认锁住，当它解锁的时候，意味着通道关闭
     */
    private val closeMutex = Mutex(true)
    private val channel1 = Channel<T1>()
    private val channel2 = Channel<T2>()
    val port1 = NativePort(channel1, channel2, closeMutex)
    val port2 = NativePort(channel2, channel1, closeMutex)
}