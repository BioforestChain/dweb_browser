package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import android.webkit.WebMessage
import android.webkit.WebMessagePort
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.helper.printerrln
import info.bagen.rust.plaoc.microService.ipc.*
import kotlinx.coroutines.*

inline fun debugMessagePortIpc(tag: String, msg: Any = "", err: Throwable? = null) =
    printdebugln("message-port-ipc", tag, msg, err)

open class MessagePortIpc(
    val port: WebMessagePort,
    override val remote: MicroModule,
    override val role: IPC_ROLE,
) : Ipc() {

    init {
        val ipc = this;
        GlobalScope.launch {
            while (true) {
                port.postMessage(WebMessage("ping"))
                delay(30000)
            }
        }
        port.setWebMessageCallback(object :
            WebMessagePort.WebMessageCallback() {
            override fun onMessage(port: WebMessagePort, event: WebMessage) {
                CoroutineScope(CoroutineName(this.toString()) + Dispatchers.IO + CoroutineExceptionHandler { ctx, e ->
                    printerrln("$ctx/$ipc", e.message, e)
                }).launch {
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

