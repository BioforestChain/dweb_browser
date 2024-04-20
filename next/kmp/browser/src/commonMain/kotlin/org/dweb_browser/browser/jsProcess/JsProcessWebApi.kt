package org.dweb_browser.browser.jsProcess

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.dwebview.ipcWeb.saveJsBridgeIpcEndpoint
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.build
import org.dweb_browser.helper.resolvePath

@Serializable
data class ProcessInfo(val process_id: Int) {
  @Transient
  lateinit var port: IWebMessagePort
    internal set
}

data class RunProcessMainOptions(val main_url: String)
class JsProcessWebApi(internal val dWebView: IDWebView) {

  /**
   * 执行js"多步骤"代码时的并发编号
   */
  private var hidAcc by SafeInt(1);

  /**
   * 创建一个jsWorker线程
   */
  suspend fun createProcess(
    env_script_url: String,
    metadata_json: String,
    env_json: String,
    host: String,
  ): ProcessInfo {
    val channel = dWebView.createMessageChannel()
    val port1 = channel.port1
    val port2 = channel.port2
    val metadata_json_str = Json.encodeToString(metadata_json)
    val env_json_str = Json.encodeToString(env_json)

    val hid = hidAcc++
    val processInfo_json = dWebView.evaluateAsyncJavascriptCode("""
            new Promise((resolve,reject)=>{
                addEventListener("message", async function doCreateProcess(event) {
                    if (event.data === "js-process/create-process/$hid") {
                     try{
                        removeEventListener("message", doCreateProcess);
                        const fetch_port = event.ports[0];
                        resolve(await createProcess(`$env_script_url`,$metadata_json_str,$env_json_str,fetch_port,`$host`, ${dwebHttpGatewayServer.startServer()}`))
                        }catch(err){
                            reject(err)
                        }
                    }
                })
            })
            """.trimIndent(), afterEval = {
      try {
        dWebView.postMessage("js-process/create-process/$hid", listOf(port1))
      } catch (e: Exception) {
        e.printStackTrace()
      }
    })
    debugJsProcess("processInfo", processInfo_json)
    val info = Json.decodeFromString<ProcessInfo>(processInfo_json)
    info.port = port2
    return info
  }

  suspend fun runProcessMain(processId: Int, options: RunProcessMainOptions) {
    dWebView.evaluateAsyncJavascriptCode(
      "runProcessMain($processId, { main_url:`${options.main_url}` })"
    )
  }

  suspend fun destroyProcess(processId: Int) {
    dWebView.evaluateAsyncJavascriptCode(
      "destroyProcess($processId)"
    )
  }

  suspend fun createIpc(processId: Int, manifestJson: String) = withContext(Dispatchers.Main) {
    val channel = dWebView.createMessageChannel()
    val port1 = channel.port1
    val port2 = channel.port2
    val jsIpcPortId = saveJsBridgeIpcEndpoint(port2)
    val hid = hidAcc++
    dWebView.evaluateAsyncJavascriptCode("""
        new Promise((resolve,reject)=>{
            const prefix = "js-process/create-ipc/$hid:"
            addEventListener("message", async function doCreateIpc(event) {
                if (event.data.startsWith(prefix)) {
                  const manifest_json = event.data.slice(prefix.length);
                  try{
                    removeEventListener("message", doCreateIpc);
                    const ipc_port = event.ports[0];
                    resolve(await createIpc($processId, manifest_json, ipc_port))
                    }catch(err){
                        reject(err)
                    }
                }
            })
        })
        """.trimIndent(), afterEval = {
      dWebView.postMessage("js-process/create-ipc/$hid:$manifestJson", listOf(port1))
    })
    jsIpcPortId
  }
//
//  // 桥接两个worker
//  suspend fun bridgeIpc(process_id: Int, fromMMid: MMID, toMMid: MMID) =
//    withContext(Dispatchers.Main) {
//      val channel = dWebView.createMessageChannel()
//      val port1 = channel.port1
//      val port2 = channel.port2
//      val fromHid = hidAcc++
//      val toHid = hidAcc++
//      dWebView.evaluateAsyncJavascriptCode("""
//        new Promise((resolve,reject)=>{
//            addEventListener("message", async function doCreateIpc(event) {
//                if (event.data === "js-process/create-ipc/$fromHid") {
//                  try{
//                    removeEventListener("message", doCreateIpc);
//                    const ipc_port = event.ports[0];
//                    resolve(await createIpc($process_id, `$fromMMid`, ipc_port))
//                    }catch(err){
//                        reject(err)
//                    }
//                } else if (event.data === "js-process/bridge-ipc/$toHid") {
//                  try{
//                    removeEventListener("message", doCreateIpc);
//                    const ipc_port = event.ports[0];
//                    resolve(await bridgeIpc(`$toMMid`, ipc_port))
//                    }catch(err){
//                        reject(err)
//                    }
//                }
//            })
//        })
//        """.trimIndent(), afterEval = {
//        dWebView.postMessage("js-process/create-ipc/$fromHid", listOf(port1))
//        dWebView.postMessage("js-process/bridge-ipc/$toHid", listOf(port2))
//      })
//      return@withContext true
//    }


  suspend fun destroy() {
    dWebView.destroy()
  }

  val onDestroy = dWebView.onDestroy
}


suspend fun createJsProcessWeb(
  mainServer: HttpDwebServer, mm: NativeMicroModule.NativeRuntime,
): JsProcessWebApi {
  /// WebView 实例
  val urlInfo = mainServer.startResult.urlInfo

  val jsProcessUrl = urlInfo.buildInternalUrl().build { resolvePath("/index.html") }.toString()
  val dWebView = IDWebView.create(mm, DWebViewOptions(privateNet = true))
  dWebView.loadUrl(jsProcessUrl)
  // 监听打开开发者工具事件
  listenOpenDevTool(dWebView, mm.getRuntimeScope())
  /// 确保API可用
  while (dWebView.evaluateAsyncJavascriptCode("typeof createProcess==='function'") == "false") {
    delay(5)
  }

  return JsProcessWebApi(dWebView)
}

/**
 * 桌面端监听打开开发者工具事件
 * TODO fuck this
 */
expect fun listenOpenDevTool(dWebView: IDWebView, scope: CoroutineScope)