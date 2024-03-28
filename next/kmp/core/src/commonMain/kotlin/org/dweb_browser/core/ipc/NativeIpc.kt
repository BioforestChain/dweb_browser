package org.dweb_browser.core.ipc

import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcPoolMessageArgs
import org.dweb_browser.core.ipc.helper.IpcPoolPack
import org.dweb_browser.helper.Debugger

val debugNativeIpc = Debugger("native-ipc")

class NativeIpc(
  val port: NativePort<IpcPoolPack, IpcPoolPack>,
  override val remote: IMicroModuleManifest,
  channelId: String,
  endpoint: IpcPool
) : Ipc(channelId, endpoint) {
  override fun toString() = "NativeIpc@($port,channelId=$channelId,remote:${remote.mmid})"

  init {
    ipcScope.launch {
      port.onMessage.collect { pack ->
        debugNativeIpc("onMessage_get", "$channelId $pack")
        endpoint.emitMessage(
          IpcPoolMessageArgs(
            IpcPoolPack(pack.pid, pack.ipcMessage),
            this@NativeIpc
          )
        )
      }
    }
  }

  override suspend fun doPostMessage(pid: Int, data: IpcMessage) {
    debugNativeIpc("postMessage_send", "$channelId $data")
    port.postMessage(IpcPoolPack(pid, data))
  }

  override suspend fun _doClose() {
    debugNativeIpc("native_doClose", "$channelId ")
  }
}
