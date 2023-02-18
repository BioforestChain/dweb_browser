package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import android.webkit.WebMessage
import android.webkit.WebMessagePort
import info.bagen.rust.plaoc.microService.MicroModule
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.moshiPack
import info.bagen.rust.plaoc.microService.ipc.*
import kotlinx.coroutines.runBlocking

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
                runBlocking {
//                    println("MessagePortIpc#port🍟message: ${event.data}")
                    when (val message = messageToIpcMessage(event.data, ipc)) {
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

