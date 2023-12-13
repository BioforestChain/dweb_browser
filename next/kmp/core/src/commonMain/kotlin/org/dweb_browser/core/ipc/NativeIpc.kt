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
import org.dweb_browser.helper.debugger
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printDebug

fun debugNativeIpc(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("native-ipc", tag, msg, err)

class NativeIpc(
  val port: NativePort<IpcMessage, IpcMessage>,
  override val remote: IMicroModuleManifest,
  private val roleType: IPC_ROLE,
) : Ipc() {
  override val role get() = roleType.role
  override fun toString(): String {
    return "NativeIpc@$port"
  }

  override val supportRaw = true
  override val supportBinary = true

  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  init {
    debugger();
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
