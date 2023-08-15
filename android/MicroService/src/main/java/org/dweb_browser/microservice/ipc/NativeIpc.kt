package org.dweb_browser.microservice.ipc

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.help.MicroModuleManifest
import org.dweb_browser.microservice.ipc.helper.IPC_ROLE
import org.dweb_browser.microservice.ipc.helper.IpcMessage
import org.dweb_browser.microservice.ipc.helper.IpcMessageArgs

fun debugNativeIpc(tag: String, msg: Any = "", err: Throwable? = null) =
  printdebugln("native-ipc", tag, msg, err)

class NativeIpc(
  val port: NativePort<IpcMessage, IpcMessage>,
  override val remote: MicroModuleManifest,
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
            port.onClose {
                close()
            }
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
