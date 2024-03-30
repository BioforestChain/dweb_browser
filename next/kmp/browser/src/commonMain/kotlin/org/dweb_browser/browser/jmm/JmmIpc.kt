package org.dweb_browser.browser.jmm

import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.IpcEndpoint
import org.dweb_browser.core.ipc.IpcPool
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcPoolMessageArgs
import org.dweb_browser.core.ipc.helper.EndpointMessage
import org.dweb_browser.core.ipc.helper.IpcReqMessage
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.ipc.helper.ipcMessageToJson
import org.dweb_browser.core.ipc.helper.jsonToIpcPack
import org.dweb_browser.core.ipc.helper.jsonToIpcPoolPack
import org.dweb_browser.core.ipc.invoke
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.ipc.helper.collectIn
import org.dweb_browser.dwebview.ipcWeb.Native2JsIpc
import org.dweb_browser.helper.Once


class JmmIpc(
  portId: Int, // 能建立桥接的messagePort
  remote: IMicroModuleManifest,
  val fromMMID: MMID, // 来建立连接的模块
  private val fetchIpc: Ipc, // 当前代理的主通道
  channelId: String, // 本次转发的名称
) : Native2JsIpc(portId, remote, channelId, kotlinIpcPool), BridgeAbleIpc {
  override val bridgeOriginIpc = this

  init {
    // 监听启动回调
    scope.launch {
      this@JmmIpc.initLifeCycleHook()
    }
    // 这里负责桥接消息，因此需要直接放行自己这边，真正的完成在worker那边的 dns/connect/done
    this.startDeferred.complete(EndpointLifecycle.open())
  }

  val toForwardIpc = Once {
    val forwardIpc = JmmForwardIpc(this, object : IMicroModuleManifest by remote {
      override var mmid = fromMMID
    }, fetchIpc, "${channelId}-forward", pool)
    forwardIpc
  }
}

/**
 * js模块在native的ipc的代理是不允许直接发送消息的，因为要将消息转发到js中再去建立通信
 */
class JmmForwardIpc(
  private val jmmIpc: JmmIpc,
  override val remote: IMicroModuleManifest,
  private val fetchIpc: Ipc,
  channelId: String,
  pool: IpcPool
) : IpcEndpoint(), BridgeAbleIpc {
  override val bridgeOriginIpc = jmmIpc
  private val lifeCycleEventName = "forward/lifeCycle/${jmmIpc.fromMMID}"
  private val requestEventName = "forward/request/${jmmIpc.fromMMID}"
  private val responseEventName = "forward/response/${jmmIpc.fromMMID}"
  private val closeEventName = "forward/close/${jmmIpc.fromMMID}"
  override val scope = fetchIpc.scope + Job()+ SupervisorJob()

  pool.scope, channelId, pool

  init {
    // 这里脱离于ipcPool 需要单独启动,放行jsMicroModule路由适配器中的beConnect
    scope.launch {
      this@JmmForwardIpc.start()
    }
    // 收到代理的消息回复
    fetchIpc.onEvent.collectIn(scope) { (ipcEvent) ->
      if (ipcEvent.name == responseEventName || ipcEvent.name == lifeCycleEventName) {
        val pack = jsonToIpcPack(ipcEvent.text)
        val message = jsonToIpcPoolPack(pack.ipcMessage, jmmIpc)
        pool.dispatchMessage(
          IpcPoolMessageArgs(
            EndpointMessage(pack.pid, message),
            this@JmmForwardIpc
          )
        )
      }
    }
  }

  // 发送代理消息到js-worker 中
  override suspend fun doPostMessage(pid: Int, data: IpcMessage) {
    println("sendMessage JmmForwardIpc ${fetchIpc.isActivity} $data")
    // 把激活信号发送到worker
    if (data is EndpointLifecycle) {
      fetchIpc.postMessage(
        IpcEvent.fromUtf8(lifeCycleEventName, ipcMessageToJson(data))
      )
    } else if (data is IpcRequest || data is IpcReqMessage) {
      fetchIpc.postMessage(
        IpcEvent.fromUtf8(requestEventName, ipcMessageToJson(data))
      )
    } else {
      debugJsMM("forward-request", data, Exception("no support forward message"))
    }
  }

  override suspend fun _doClose() {
    debugJsMM("JmmForwardIpc close", ipcDebugId)
    if (!fetchIpc.isClosed) {
      fetchIpc.postMessage(IpcEvent.fromUtf8(closeEventName, ""))
    }
  }
}


interface BridgeAbleIpc {
  val bridgeOriginIpc: JmmIpc
}
