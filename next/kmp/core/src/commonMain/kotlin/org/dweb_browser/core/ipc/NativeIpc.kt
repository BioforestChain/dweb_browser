package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcPoolMessageArgs
import org.dweb_browser.core.ipc.helper.IpcPoolPack
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ioAsyncExceptionHandler

val debugNativeIpc = Debugger("native-ipc")

class NativeIpc(
  val port: NativePort<IpcPoolPack, IpcPoolPack>,
  override val remote: IMicroModuleManifest,
  channelId: String,
  endpoint: IpcPool
) : Ipc(channelId, endpoint) {
  override fun toString() = "NativeIpc@($port,channelId=$channelId,remote:${remote.mmid})"

  // 这里放在协程
  private val ioAsyncScope = CoroutineScope(CoroutineName("native-ipc") + ioAsyncExceptionHandler)

  init {
    port.onMessage { pack ->
      debugNativeIpc("onMessage", "$channelId $pack")
      endpoint.emitMessage(
        IpcPoolMessageArgs(
          IpcPoolPack(pack.pid, pack.ipcMessage),
          this@NativeIpc
        )
      )
    }
    ioAsyncScope.launch {
      port.onClose { close() }
      port.start()
    }
  }


  override suspend fun doPostMessage(pid: Int, data: IpcMessage) {
    debugNativeIpc("postMessage send", "$channelId $data")
    port.postMessage(IpcPoolPack(pid, data))
  }


  override suspend fun doClose() {
    port.close()
    ioAsyncScope.cancel()
  }
}
