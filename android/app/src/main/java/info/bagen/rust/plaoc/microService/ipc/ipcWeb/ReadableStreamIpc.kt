package info.bagen.rust.plaoc.microService.ipc.ipcWeb

import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.helper.IpcMessage

class ReadableStreamIpc() :Ipc() {
    override val supportMessagePack: Boolean = false
    override fun _doPostMessage(data: IpcMessage): Void {
        TODO("Not yet implemented")
    }

    override fun _doClose(): Void {
        TODO("Not yet implemented")
    }
}