package org.dweb_browser.core.ipc

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.IPC_ROLE
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcMessageArgs
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ioAsyncExceptionHandler

val debugNativeIpc = Debugger("native-ipc")

class NativeIpc(
  val port: NativePort<IpcMessage, IpcMessage>,
  override val remote: IMicroModuleManifest,
  private val roleType: IPC_ROLE,
) : Ipc() {
  override val role get() = roleType.role
  override fun toString() = "NativeIpc@($port)"

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


  override suspend fun _doPostMessage(data: IpcMessage) =
    withContext(ioAsyncScope.coroutineContext) {
      port.postMessage(data)
    }

  override suspend fun _doClose() {
    port.close()
    ioAsyncScope.cancel()
  }
}
