package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.helper.*

class ReadableStreamIpc :Ipc() {
    override val supportMessagePack: Boolean = false
    override fun _doPostMessage(data: info.bagen.rust.plaoc.microService.ipc.IpcMessage) {
        TODO("Not yet implemented")
    }

    override fun _doClose() {
        TODO("Not yet implemented")
    }
}