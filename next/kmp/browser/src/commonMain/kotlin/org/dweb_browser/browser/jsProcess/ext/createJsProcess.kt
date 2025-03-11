package org.dweb_browser.browser.jsProcess.ext

import io.ktor.http.URLBuilder
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.jmm.debugJsMM
import org.dweb_browser.browser.jsProcess.CreateProcessReturn
import org.dweb_browser.browser.kit.GlobalWebMessageEndpoint
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.http.router.HandlerContext
import org.dweb_browser.core.http.router.HttpHandlerToolkit
import org.dweb_browser.core.http.router.HttpRouter
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.collectIn

suspend fun MicroModule.Runtime.createJsProcess(
  entryPath: String,
  processName: String?,
): JsProcess {
  val mainIpc = this.connect("js.browser.dweb")
  mainIpc.start()
  val codeIpc = mainIpc.fork(autoStart = true)
  val result = codeIpc.request(URLBuilder("file://js.browser.dweb/create-process").run {
    processName?.also { parameters["name"] = processName }
    parameters["entry"] = entryPath
    buildUnsafeString()
  }).json<CreateProcessReturn>()
  val fetchIpc = kotlinIpcPool.createIpc(
    endpoint = GlobalWebMessageEndpoint.get(result.portId),
    pid = 0,
    locale = microModule.manifest,
    remote = microModule.manifest,
    autoStart = true,
    startReason = "create-fetch-ipc"
  )
  codeIpc.onClosed {
    codeIpc.launchJobs += codeIpc.scope.launch(start = CoroutineStart.UNDISPATCHED) { fetchIpc.close() }
  }
  return JsProcess(result.processToken, this, codeIpc, fetchIpc)
}

class JsProcess(
  private val processToken: String,
  private val runtime: MicroModule.Runtime,
  val codeIpc: Ipc,
  val fetchIpc: Ipc,
) {

  /**
   * 使用路由定义 esm 模块的代码内容
   */
  fun defineEsm(definer: HttpHandlerToolkit.() -> Unit) {
    val httpRouter = HttpRouter(runtime, runtime.mmid)
    val toolkit = object : HttpHandlerToolkit {
      override fun getContextHttpRouter(): HttpRouter {
        return httpRouter
      }
    }
    // 定义完路由后
    toolkit.definer()
    // 再进行消费
    codeIpc.onRequest("js-process-http").collectIn(codeIpc.scope) {
      it.consumeFilter { ipcServerRequest ->
        val pureServerRequest = ipcServerRequest.toPure()
        val httpHandlerChain =
          httpRouter.withFilter(pureServerRequest) ?: return@consumeFilter false
        val pureResponse = httpHandlerChain(HandlerContext(pureServerRequest, codeIpc))
        codeIpc.postResponse(ipcServerRequest.reqId, pureResponse)
        return@consumeFilter true
      }
    }
  }

  /**
   * 创建一个可以与jsProcess内部通讯的ipc
   */
  suspend fun createIpc(remoteMM: MicroModuleManifest): Ipc {
    val globalId =
      runtime.nativeFetch(buildUrlString("file://js.browser.dweb/create-ipc-endpoint") {
        parameters["token"] = processToken
        parameters["manifest"] = Json.encodeToString(remoteMM)
      }).int()
    debugJsMM("create-ipc") { "globalId=$globalId,processToken=$processToken" }
    return kotlinIpcPool.createIpc(
      endpoint = GlobalWebMessageEndpoint.get(globalId),
      pid = 0,
      remote = runtime.microModule.manifest,
      locale = remoteMM,
      // 不自动开始，等到web-worker中它自己去握手
      autoStart = false,
      startReason = "create-ipc-endpoint",
    )
  }

  suspend fun bridgeIpc(remoteGlobalId: Int, remoteMM: MicroModuleManifest) {
    runtime.nativeFetch(buildUrlString("file://js.browser.dweb/create-ipc") {
      parameters["token"] = processToken
      parameters["globalId"] = remoteGlobalId.toString()
      parameters["manifest"] = Json.encodeToString(remoteMM)
    })
  }
}