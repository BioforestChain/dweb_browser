package org.dweb_browser.browser.jsProcess

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.core.ipc.MessagePort
import org.dweb_browser.core.ipc.MessagePortIpc
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.dwebview.ipcWeb.saveNative2JsIpcPort
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.build
import org.dweb_browser.helper.resolvePath

@Serializable
data class ProcessInfo(val process_id: Int)
class ProcessHandler(val info: ProcessInfo, var ipc: MessagePortIpc)
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
    remoteModule: IMicroModuleManifest,
    host: String
  ): ProcessHandler {
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
                        resolve(await createProcess(`$env_script_url`,$metadata_json_str,$env_json_str,fetch_port,`$host`,`{"jsMicroModule":"${JsMicroModule.VERSION}.${JsMicroModule.PATCH}"}, ${dwebHttpGatewayServer.startServer()}`))
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
    val ipc = kotlinIpcPool.create(
      "create-process-${remoteModule.mmid}",
      remoteModule,
      MessagePort.from(port2),
    )
    return ProcessHandler(info, ipc)
  }

  suspend fun runProcessMain(process_id: Int, options: RunProcessMainOptions) {
    dWebView.evaluateAsyncJavascriptCode(
      "runProcessMain($process_id, { main_url:`${options.main_url}` })"
    )
  }

  suspend fun destroyProcess(process_id: Int) {
    dWebView.evaluateAsyncJavascriptCode(
      "destroyProcess($process_id)"
    )
  }

  suspend fun createIpc(process_id: Int, mmid: MMID) = withContext(Dispatchers.Main) {
    val channel = dWebView.createMessageChannel()
    val port1 = channel.port1
    val port2 = channel.port2
    val jsIpcPortId = saveNative2JsIpcPort(port2)
    val hid = hidAcc++
    dWebView.evaluateAsyncJavascriptCode("""
        new Promise((resolve,reject)=>{
            addEventListener("message", async function doCreateIpc(event) {
                if (event.data === "js-process/create-ipc/$hid") {
                  try{
                    removeEventListener("message", doCreateIpc);
                    const ipc_port = event.ports[0];
                    resolve(await createIpc($process_id, `$mmid`, ipc_port))
                    }catch(err){
                        reject(err)
                    }
                }
            })
        })
        """.trimIndent(), afterEval = {
      dWebView.postMessage("js-process/create-ipc/$hid", listOf(port1))
    })
    jsIpcPortId
  }

  // 桥接两个worker
  suspend fun bridgeIpc(process_id: Int, fromMMid: MMID, toMMid: MMID) =
    withContext(Dispatchers.Main) {
      val channel = dWebView.createMessageChannel()
      val port1 = channel.port1
      val port2 = channel.port2
      val fromHid = hidAcc++
      val toHid = hidAcc++
      dWebView.evaluateAsyncJavascriptCode("""
        new Promise((resolve,reject)=>{
            addEventListener("message", async function doCreateIpc(event) {
                if (event.data === "js-process/create-ipc/$fromHid") {
                  try{
                    removeEventListener("message", doCreateIpc);
                    const ipc_port = event.ports[0];
                    resolve(await createIpc($process_id, `$fromMMid`, ipc_port))
                    }catch(err){
                        reject(err)
                    }
                } else if (event.data === "js-process/bridge-ipc/$toHid") {
                  try{
                    removeEventListener("message", doCreateIpc);
                    const ipc_port = event.ports[0];
                    resolve(await bridgeIpc(`$toMMid`, ipc_port))
                    }catch(err){
                        reject(err)
                    }
                }
            })
        })
        """.trimIndent(), afterEval = {
        dWebView.postMessage("js-process/create-ipc/$fromHid", listOf(port1))
        dWebView.postMessage("js-process/bridge-ipc/$toHid", listOf(port2))
      })
      return@withContext true
    }


  suspend fun destroy() {
    dWebView.destroy()
  }

  val onDestroy = dWebView.onDestroy
}


suspend fun createJsProcessWeb(
  mainServer: HttpDwebServer,
  mm: NativeMicroModule
): JsProcessWebApi {
  /// WebView 实例
  val urlInfo = mainServer.startResult.urlInfo

  val jsProcessUrl = urlInfo.buildInternalUrl().build { resolvePath("/index.html") }.toString()
  val dWebView = IDWebView.create(mm, DWebViewOptions(privateNet = true))
  dWebView.loadUrl(jsProcessUrl)
  // 监听打开开发者工具事件
  listenOpenDevTool(dWebView, mm.ioAsyncScope)
  /// 确保API可用
  while (dWebView.evaluateAsyncJavascriptCode("typeof createProcess==='function'") == "false") {
    delay(5)
  }

  return JsProcessWebApi(dWebView)
}

/**
 *  桌面端监听打开开发者工具事件
 */
expect fun listenOpenDevTool(dWebView: IDWebView, scope: CoroutineScope)