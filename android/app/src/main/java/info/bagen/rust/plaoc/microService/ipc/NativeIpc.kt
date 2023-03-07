package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

inline fun debugNativeIpc(tag: String, msg: Any = "", err: Throwable? = null) =
    printdebugln("native-ipc", tag, msg, err)

class NativeIpc(
    val port: NativePort<IpcMessage, IpcMessage>,
    override val remote: MicroModuleInfo,
    private val role_type: IPC_ROLE,
) : Ipc() {
    override val role get() = role_type.role
    override fun toString(): String {
        return super.toString() + "@NativeIpc"
    }

    override val supportRaw = true
    override val supportBinary = true

    init {
        port.onMessage { message ->
//            val ipcMessage = when (message.type) {
//                IPC_DATA_TYPE.REQUEST -> (message as IpcRequest).let { fromRequest ->
//                    /**
//                     * fromRequest 携带者远端的 ipc 对象，不是我们的 IpcRequest 对象。
//                     */
//                    IpcRequest.fromRequest(fromRequest.req_id, fromRequest.toRequest(), this)
//                }
//                IPC_DATA_TYPE.RESPONSE -> (message as IpcResponse).let { fromResponse ->
//                    /**
//                     * fromResponse 携带者远端的 ipc 对象，不是我们的 IpcResponse 对象。
//                     */
//                    IpcResponse.fromResponse(fromResponse.req_id, fromResponse.toResponse(), this)
//                }
//                /**
//                 * 其它情况的对象可以直接复用
//                 */
//                else -> message
//            }
//            _messageSignal.emit(IpcMessageArgs(ipcMessage, this@NativeIpc))
//                debugNativeIpc("onMessage/emitted $message")
            _messageSignal.emit(IpcMessageArgs(message, this@NativeIpc))
        }
        GlobalScope.launch(ioAsyncExceptionHandler) {
            port.start()
        }
    }


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
    private val closePo: PromiseOut<Unit>
) {
    companion object {
        private var uid_acc = 1;
    }

    private val uid = uid_acc++
    override fun toString() = "#p$uid"

    private var started = false
    suspend fun start() {
        if (started || closePo.isFinished) return else started = true

        debugNativeIpc("port-message-start/$this")
        for (message in channel_in) {
            debugNativeIpc("port-message-in/$this << $message")
            _messageSignal.emit(message)
            debugNativeIpc("port-message-waiting/$this")
        }
        debugNativeIpc("port-message-end/$this")
    }


    private val _closeSignal = SimpleSignal()

    fun onClose(cb: SimpleCallback) = _closeSignal.listen(cb)

    fun close() {
        if (!closePo.isFinished) {
            closePo.resolve(Unit)
            debugNativeIpc("port-closing/$this")
        }
    }

    /**
     * 等待 close 信号被发出，那么就关闭出口、触发事件
     */
    init {
        GlobalScope.launch(ioAsyncExceptionHandler) {
            closePo.waitPromise()
            channel_out.close()
            _closeSignal.emit()
            debugNativeIpc("port-closed/${this@NativePort}")
        }

    }


    private val _messageSignal = Signal<I>()

    /**
     * 发送消息，这个默认会阻塞
     */
    suspend fun postMessage(msg: O) {
        debugNativeIpc("message-out/$this >> $msg")
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
    private val closePo = PromiseOut<Unit>()
    private val channel1 = Channel<T1>()
    private val channel2 = Channel<T2>()
    val port1 = NativePort(channel1, channel2, closePo)
    val port2 = NativePort(channel2, channel1, closePo)
}