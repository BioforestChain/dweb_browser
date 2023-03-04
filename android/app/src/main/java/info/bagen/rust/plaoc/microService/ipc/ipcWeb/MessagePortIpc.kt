package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import android.webkit.WebMessage
import android.webkit.WebMessagePort
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.ioAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.ipc.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

inline fun debugMessagePortIpc(tag: String, msg: Any = "", err: Throwable? = null) =
    printdebugln("message-port-ipc", tag, msg, err)

open class MessagePortIpc(
    val port: WebMessagePort,
    override val remote: MicroModule,
    private val role_type: IPC_ROLE,
) : Ipc() {
    override val role get() = role_type.role
    override fun toString(): String {
        return super.toString() + "@MessagePortIpc"
    }

    init {
        val ipc = this;
//        GlobalScope.launch {
//            while (true) {
//                port.postMessage(WebMessage("ping"))
//                delay(30000)
//            }
//        }
        port.setWebMessageCallback(object :
            WebMessagePort.WebMessageCallback() {
            override fun onMessage(port: WebMessagePort, event: WebMessage) {
                CoroutineScope(CoroutineName(this.toString()) + ioAsyncExceptionHandler).launch {
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
            }
        })
    }

    override suspend fun _doPostMessage(data: IpcMessage) {
        val message = when (data) {
            is IpcRequest -> gson.toJson(data.ipcReqMessage)
            is IpcResponse -> gson.toJson(data.ipcResMessage)
            else -> gson.toJson(data)
        }
        this.port.postMessage(WebMessage(message))
    }

    override suspend fun _doClose() {
        this.port.postMessage(WebMessage("close"))
        this.port.close()
    }
}

