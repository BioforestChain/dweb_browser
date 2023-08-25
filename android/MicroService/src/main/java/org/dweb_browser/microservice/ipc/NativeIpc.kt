package org.dweb_browser.microservice.ipc

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.help.MicroModuleManifest
import org.dweb_browser.microservice.ipc.helper.IPC_ROLE
import org.dweb_browser.microservice.ipc.helper.IpcMessage
import org.dweb_browser.microservice.ipc.helper.IpcMessageArgs

fun debugNativeIpc(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("native-ipc", tag, msg, err)

class NativeIpc(
  val port: NativePort<IpcMessage, IpcMessage>,
  override val remote: MicroModuleManifest,
  private val roleType: IPC_ROLE,
) : Ipc() {
  override val role get() = roleType.role
  override fun toString(): String {
    return super.toString() + "@NativeIpc"
  }

  override val supportRaw = true
  override val supportBinary = true

  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  init {
    port.onMessage { message ->
      _messageSignal.emit(IpcMessageArgs(message, this@NativeIpc))
    }
    ioAsyncScope.launch {
      port.onClose { close() }
      port.start()
    }
  }


  override suspend fun _doPostMessage(data: IpcMessage) {
    port.postMessage(data)
  }

  override suspend fun _doClose() {
    port.close()
    ioAsyncScope.cancel()
  }
}
