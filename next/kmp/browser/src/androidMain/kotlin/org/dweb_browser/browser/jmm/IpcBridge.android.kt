package org.dweb_browser.browser.jmm

import io.ktor.http.URLBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.ipcWeb.Native2JsIpc
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.buildUnsafeString

class JmmIpc(portId: Int, remote: IMicroModuleManifest) : Native2JsIpc(portId, remote)

actual suspend fun ipcBridge(
  fromMMID: MMID,
  remoteMM: MicroModule,
  pid: String,
  fromMMIDOriginIpcWM: MutableMap<MMID, PromiseOut<Ipc>>,
  targetIpc: Ipc?
) = withContext(remoteMM.ioAsyncScope.coroutineContext) {
  fromMMIDOriginIpcWM.getOrPut(fromMMID) {
    PromiseOut<Ipc>().also { po ->
      remoteMM.ioAsyncScope.launch {
        try {

          debugJsMM("ipcBridge", "fromMmid:$fromMMID targetIpc:$targetIpc")
          /**
           * 向js模块发起连接
           */
          /**
           * 向js模块发起连接
           */
          val portId = remoteMM.nativeFetch(
            URLBuilder("file://js.browser.dweb/create-ipc").apply {
              parameters["process_id"] = pid
              parameters["mmid"] = fromMMID
            }.buildUnsafeString()
          ).int()
          val originIpc = JmmIpc(portId, remoteMM)

          /// 如果传入了 targetIpc，那么启动桥接模式，我们会中转所有的消息给 targetIpc，包括关闭，那么这个 targetIpc 理论上就可以作为 originIpc 的代理
          if (targetIpc != null) {
            /**
             * 将两个消息通道间接互联
             */
            /**
             * 将两个消息通道间接互联
             */
            originIpc.onMessage { (ipcMessage) ->
              targetIpc.postMessage(ipcMessage)
            }
            targetIpc.onMessage { (ipcMessage) ->
              originIpc.postMessage(ipcMessage)
            }
            /**
             * 监听关闭事件
             */
            /**
             * 监听关闭事件
             */
            originIpc.onClose {
              fromMMIDOriginIpcWM.remove(targetIpc.remote.mmid)
              targetIpc.close()
            }
            targetIpc.onClose {
              fromMMIDOriginIpcWM.remove(originIpc.remote.mmid)
              originIpc.close()
            }
          } else {
            originIpc.onClose {
              fromMMIDOriginIpcWM.remove(originIpc.remote.mmid)
            }
          }
          po.resolve(originIpc);
        } catch (e: Exception) {
          debugJsMM("_ipcBridge Error", e)
          po.reject(e)
        }
      }
    }
  }.waitPromise()
}
