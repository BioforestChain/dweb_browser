package org.dweb_browser.browser.jsProcess

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.kit.GlobalWebMessageEndpoint
import org.dweb_browser.core.http.dwebHttpGatewayService
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.build
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.pure.http.onPortChange

@Serializable
data class ProcessInfo(@SerialName("process_id") val processId: Int, var portId: Int = -1)

@Serializable
data class CreateProcessReturn(val processToken: UUID, val portId: Int)

data class RunProcessMainOptions(val main_url: String)
class JsProcessWebApi(internal val dWebView: IDWebView) {
  init {
    dwebHttpGatewayService.server.onPortChange("updateGatewayPort", false) { port ->
      debugJsProcess("updateGatewayPort/start", port)
      runCatching {
        dWebView.evaluateAsyncJavascriptCode(
          "updateGatewayPort($port)"
        )
      }.getOrElse {
        debugJsProcess("updateGatewayPort/error", port, it)
      }
      debugJsProcess("updateGatewayPort/end", port)
    }
  }

  /**
   * 执行js"多步骤"代码时的并发编号
   */
  private var hidAcc by SafeInt(1);

  /**
   * 创建一个jsWorker线程
   */
  suspend fun createProcess(
    processName: String,
    envScriptUrl: String,
    metadataJson: String,
    envJson: String,
    onTerminate: suspend () -> Unit,
  ): ProcessInfo {
    debugJsProcess("createProcess") {
      """
      ---
      processName=$processName
      envScriptUrl=$envScriptUrl
      metadataJson=$metadataJson
      envJson=$envJson
      ---
      """.trimIndent()
    }
    val channel = dWebView.createMessageChannel()
    val port1 = channel.port1
    val port2 = channel.port2
    val metadataJsonStr = Json.encodeToString(metadataJson)
    val envJsonStr = Json.encodeToString(envJson)
    val processNameStr = Json.encodeToString(processName)
    val gatewayPort = dwebHttpGatewayService.getPort()

    val onTerminateCallbackId = "onTerminate-${randomUUID()}"
    val onTerminateCallbackReady = CompletableDeferred<Unit>()
    dWebView.lifecycleScope.launch {
      dWebView.evaluateAsyncJavascriptCode("(window['$onTerminateCallbackId'] = new PromiseOut()).promise") {
        onTerminateCallbackReady.complete(Unit)
      }
      onTerminate()
      port2.close()
    }
    onTerminateCallbackReady.await()

    val hid = hidAcc++
    val processInfoJson = dWebView.evaluateAsyncJavascriptCode("""
      new Promise((resolve,reject)=>{
        addEventListener("message", async function doCreateProcess(event) {
          if (event.data === "js-process/create-process/$hid") {
            try{
              removeEventListener("message", doCreateProcess);
              const fetch_port = event.ports[0];
              const on_terminate_po = window['$onTerminateCallbackId'];
              delete window['$onTerminateCallbackId'];
              const process = await createProcess($processNameStr,`$envScriptUrl`,$metadataJsonStr,$envJsonStr,fetch_port,$gatewayPort,()=>{
                on_terminate_po.resolve()
              }); 
              resolve(process)
            } catch (err) {
              reject(err)
            }
          }
        })
      })
      """.trimIndent(), afterEval = {
      try {
        dWebView.postMessage("js-process/create-process/$hid", listOf(port1))
        port1.unref()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    })
    debugJsProcess("processInfo", processInfoJson)
    val info = Json.decodeFromString<ProcessInfo>(processInfoJson)
    info.portId = GlobalWebMessageEndpoint(port2, "fetch-ipc:$processName").globalId
    return info
  }

  suspend fun runProcessMain(processId: Int, options: RunProcessMainOptions) {
    dWebView.evaluateAsyncJavascriptCode(
      "runProcessMain($processId, { main_url:`${options.main_url}` })"
    )
  }

  suspend fun destroyProcess(processId: Int) {
    runCatching {
      dWebView.evaluateAsyncJavascriptCode(
        "destroyProcess($processId)"
      )
    }.getOrNull()
  }

  /**
   * 创建一对 ipc-endpoint，并同时将其中一个 endpoint 用来创建ipc，剩下的这个endpoint用来返回
   */
  suspend fun createIpcEndpoint(
    processId: Int,
    manifestJson: String,
    debugIdPrefix: String,
    autoStart: Boolean? = null,
  ) = withMainContext {
    val channel = dWebView.createMessageChannel()
    createJsIpc(processId, channel.port1, manifestJson, autoStart) {
      channel.port2.close()
    }
    GlobalWebMessageEndpoint(channel.port2, debugIdPrefix)
  }

  /**
   * 提供指定的endpoint，在 js 中创建一个 ipc
   */
  suspend fun createJsIpc(
    processId: Int,
    port: IWebMessagePort,
    manifestJson: String,
    autoStart: Boolean? = null,
    onClose: suspend () -> Unit,
  ) {
    val onCloseCallbackId = "onClose-${randomUUID()}"
    // 连接方关闭
    val onTerminateCallbackReady = CompletableDeferred<Unit>()
    dWebView.lifecycleScope.launch {
      dWebView.evaluateAsyncJavascriptCode("(window['$onCloseCallbackId'] = new PromiseOut()).promise") {
        onTerminateCallbackReady.complete(Unit)
      }
      onClose()
    }
    onTerminateCallbackReady.await()

    withMainContext {
      val hid = hidAcc++
      dWebView.evaluateAsyncJavascriptCode("""
        new Promise((resolve,reject)=>{
            const prefix = "js-process/create-ipc/$hid:"
            addEventListener("message", async function doCreateIpc(event) {
                if (event.data.startsWith(prefix)) {
                  const manifest_json = event.data.slice(prefix.length);
                  try {
                      removeEventListener("message", doCreateIpc);
                      const ipc_port = event.ports[0];
                      const on_close_po = window['$onCloseCallbackId'];
                      delete window['$onCloseCallbackId'];
                      resolve(await createIpc($processId, manifest_json, ipc_port, ${autoStart ?: "undefined"}, ()=>{
                        on_close_po.resolve()
                      }))
                  } catch (err) {
                      reject(err)
                  }
                }
            })
        })
        """.trimIndent(), afterEval = {
        dWebView.postMessage("js-process/create-ipc/$hid:$manifestJson", listOf(port))
        port.unref()
      })
    }
  }

  suspend fun destroy() {
    dWebView.destroy()
  }

  val onDestroy = dWebView.onDestroy
}


suspend fun createJsProcessWeb(
  mainServer: HttpDwebServer, mm: NativeMicroModule.NativeRuntime,
): JsProcessWebApi {
  debugJsProcess("createJsProcessWeb")
  /// WebView 实例
  val urlInfo = mainServer.startResult.urlInfo

  val jsProcessUrl = urlInfo.buildInternalUrl().build { resolvePath("/index.html") }.toString()
  val dWebView = IDWebView.create(
    mm, DWebViewOptions(
      privateNet = true,
      openDevTools = envSwitch.isEnabled(ENV_SWITCH_KEY.JS_PROCESS_DEVTOOLS),
    )
  )
  // 等待加载完成
  dWebView.loadUrl(jsProcessUrl)
  /// 确保API可用
  while (dWebView.evaluateAsyncJavascriptCode("typeof createProcess==='function'") == "false") {
    delay(5)
  }

  return JsProcessWebApi(dWebView)
}
