package org.dweb_browser.microservice.ipc

import org.dweb_browser.helper.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.dweb_browser.microservice.ipc.message.IPC_ROLE
import org.dweb_browser.microservice.ipc.message.IpcMessage
import org.dweb_browser.microservice.ipc.message.IpcMessageArgs
import java.util.concurrent.atomic.AtomicInteger

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
        private var uid_acc = AtomicInteger(1);
    }

    private val uid = uid_acc.getAndAdd(1)
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
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun postMessage(msg: O) {
        debugNativeIpc("message-out/$this >> $msg")
        if (!channel_out.isClosedForSend) {
            channel_out.send(msg)
        } else {
            debugNativeIpc("postMessage"," handle the closed channel case!")
        }
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