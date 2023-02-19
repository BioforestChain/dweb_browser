package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import android.webkit.WebMessage
import android.webkit.WebMessagePort
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcMessage
import info.bagen.rust.plaoc.microService.ipc.IpcMessageArgs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class MessagePortIpc(
    val port: WebMessagePort,
    override val remote: MicroModule,
    override val role: IPC_ROLE,
) : Ipc() {

    init {
        val ipc = this
        port.setWebMessageCallback(object :
            WebMessagePort.WebMessageCallback() {
            override fun onMessage(port: WebMessagePort, event: WebMessage) {
                GlobalScope.launch {
//                    println("MessagePortIpc#port🍟message: ${event.data}")
                    when (val message = jsonToIpcMessage(event.data, ipc)) {
                        "close" -> close()
                        is IpcMessage -> _messageSignal.emit(IpcMessageArgs(message, ipc))
                    }
                }
            }
        })
    }

    override suspend fun _doPostMessage(data: IpcMessage) {
        this.port.postMessage(WebMessage(gson.toJson(data)))
    }

    override suspend fun _doClose() {
        this.port.postMessage(WebMessage("close"))
        this.port.close()
    }
}

