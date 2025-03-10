package org.dweb_browser.core.ipc.helper

import io.ktor.util.InternalAPI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.LateInit
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.buildRequestX

class IpcServerRequest(
  reqId: Int,
  url: String,
  method: PureMethod,
  headers: PureHeaders,
  body: IpcBody,
  ipc: Ipc,
  override val from: Any? = null,
) : IpcRequest(
  reqId = reqId, url = url, method = method, headers = headers, body = body, ipc = ipc
) {

  override val typeTag = "IpcServerRequest"
  fun getClient() = findFrom { if (it is IpcClientRequest) it else null }

  internal val pure = LateInit<PureServerRequest>()

  @OptIn(InternalAPI::class)
  suspend fun toPure() = pure.getOrInit {
    buildRequestX(url, method, headers, body.raw, from = this).let { pureRequest ->
      /// 如果存在双工通道，那么这个 pureRequest 用不了，需要重新构建一个新的 PureServerRequest
      if (hasDuplex) {
        val debugTag = "PureServer/ipcToChannel"
        val forkedIpcId = duplexIpcId!!
        ipc.debugIpc(debugTag) { "waitForkedIpc=$forkedIpcId" }
        val channelIpc = ipc.waitForkedIpc(forkedIpcId)
        ipc.debugIpc(debugTag) { "forkedIpc=$channelIpc" }

        val pureChannel = PureChannel()
        val pureChannelDeferred = CompletableDeferred(pureChannel)
        ipc.scope.launch(start = CoroutineStart.UNDISPATCHED) {
          pureChannelToIpcEvent(
            channelIpc,
            pureChannelDeferred = pureChannelDeferred,
            debugTag = debugTag,
          )
        }

        PureServerRequest(
          href = pureRequest.href,
          method = pureRequest.method,
          headers = headers.copy().apply { delete(X_IPC_UPGRADE_KEY) },
          body = pureRequest.body,
          channel = pureChannelDeferred,
          from = pureRequest.from,
        ).also { pureServerRequest ->
          pureChannel.from = pureServerRequest
        }
      } else pureRequest.toServer()
    }
  }
}