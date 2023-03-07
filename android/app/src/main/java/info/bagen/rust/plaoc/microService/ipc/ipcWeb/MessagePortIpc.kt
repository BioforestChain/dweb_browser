package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import android.webkit.WebMessage
import android.webkit.WebMessagePort
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.ipc.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

inline fun debugMessagePortIpc(tag: String, msg: Any = "", err: Throwable? = null) =
    printdebugln("message-port-ipc", tag, msg, err)

class MessagePort {
    companion object {
        private val wm = WeakHashMap<WebMessagePort, MessagePort>()
        fun from(port: WebMessagePort): MessagePort = wm.getOrPut(port) { MessagePort(port) }
    }

    private lateinit var port: WebMessagePort

    private constructor(port: WebMessagePort) {
        this.port = port
    }

    private val _messageSignal by lazy {
        val signal = Signal<WebMessage>()

        port.setWebMessageCallback(object :
            WebMessagePort.WebMessageCallback() {
            override fun onMessage(port: WebMessagePort, event: WebMessage) {
                GlobalScope.launch(ioAsyncExceptionHandler) {
                    signal.emit(event)
                }
            }
        })

        signal
    }

    fun onWebMessage(cb: Callback<WebMessage>) = _messageSignal.listen(cb)
    fun postMessage(event: WebMessage) = port.postMessage(event)

    fun close() = port.close()
}

open class MessagePortIpc(
    val port: MessagePort,
    override val remote: MicroModuleInfo,
    private val role_type: IPC_ROLE,
) : Ipc() {
    constructor(
        port: WebMessagePort,
        remote: MicroModuleInfo,
        role_type: IPC_ROLE
    ) : this(MessagePort.from(port), remote, role_type)

    override val role get() = role_type.role
    override fun toString(): String {
        return super.toString() + "@MessagePortIpc"
    }

    init {
        val ipc = this;
        val callback = port.onWebMessage { event ->
            when (val message = jsonToIpcMessage(event.data, ipc)) {
                "close" -> close()
                "ping" -> port.postMessage(WebMessage("pong"))
                "pong" -> debugMessagePortIpc("PONG/$ipc")
                is IpcMessage -> {
                    debugMessagePortIpc("ON-MESSAGE/$ipc", message)
                    _messageSignal.emit(
                        IpcMessageArgs(message, ipc)
                    )
                }
                else -> throw Exception("unknown message: $message")
            }
        }
        onDestroy(callback)

    }

    override suspend fun _doPostMessage(data: IpcMessage) {
        val message = when (data) {
            is IpcRequest -> gson.toJson(data.ipcReqMessage)
            is IpcResponse -> gson.toJson(data.ipcResMessage)
            is IpcStreamData -> gson.toJson(data)
            else -> gson.toJson(data)
        }
        this.port.postMessage(WebMessage(message))
    }

    override suspend fun _doClose() {
        this.port.postMessage(WebMessage("close"))
        this.port.close()
    }
}

