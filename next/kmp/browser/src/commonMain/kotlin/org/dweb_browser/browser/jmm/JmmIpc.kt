package org.dweb_browser.browser.jmm

import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.IpcPool
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcPoolMessageArgs
import org.dweb_browser.core.ipc.helper.IpcPoolPack
import org.dweb_browser.core.ipc.helper.IpcReqMessage
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.ipc.helper.ipcPoolPackToJson
import org.dweb_browser.core.ipc.helper.jsonToIpcPack
import org.dweb_browser.core.ipc.helper.jsonToIpcPoolPack
import org.dweb_browser.dwebview.ipcWeb.Native2JsIpc
import org.dweb_browser.helper.Once


class JmmIpc(
  port_id: Int,
  remote: IMicroModuleManifest,
  val fromMMID: MMID,
  private val fetchIpc: Ipc,
  channelId: String,
  endpoint: IpcPool
) :
  Native2JsIpc(port_id, remote, channelId, endpoint), JsMicroModule.BridgeAbleIpc {
  override val bridgeOriginIpc = this
  val toForwardIpc = Once {
    JmmForwardIpc(this, object : IMicroModuleManifest by remote {
      override var mmid = fromMMID
    }, fetchIpc, channelId, endpoint)
  }
}

class JmmForwardIpc(
  private val jmmIpc: JmmIpc,
  override val remote: IMicroModuleManifest,
  private val fetchIpc: Ipc,
  channelId: String,
  endpoint: IpcPool
) : Ipc(channelId, endpoint), JsMicroModule.BridgeAbleIpc {
  override val bridgeOriginIpc = jmmIpc
  private val requestEventName = "forward/request/${jmmIpc.fromMMID}"
  private val responseEventName = "forward/response/${jmmIpc.fromMMID}"

  init {
    fetchIpc.onEvent { (ipcEvent) ->
      if (ipcEvent.name == responseEventName) {
        val pack = jsonToIpcPack(ipcEvent.text)
        val message = jsonToIpcPoolPack(pack.ipcMessageString, jmmIpc)
        endpoint.emitMessage(
          IpcPoolMessageArgs(
            IpcPoolPack(pack.pid, message),
            this@JmmForwardIpc
          )
        )
      }
    }.removeWhen(this.onClose)
  }

  override suspend fun doPostMessage(pid: Int, data: IpcMessage) {
    if (data is IpcRequest || data is IpcReqMessage) {
      fetchIpc.postMessage(
        IpcEvent.fromUtf8(requestEventName, ipcPoolPackToJson(IpcPoolPack(pid, data)))
      )
    } else {
      debugJsMM("forward-request", data, Exception("no support forward message"))
    }
  }

  override suspend fun doClose() {
    fetchIpc.postMessage(IpcEvent.fromUtf8("forward/close/${jmmIpc.fromMMID}", ""))
  }
}
