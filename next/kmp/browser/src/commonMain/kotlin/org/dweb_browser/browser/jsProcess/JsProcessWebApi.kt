package org.dweb_browser.browser.jsProcess

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.ipc.helper.IPC_ROLE
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.dwebview.ipcWeb.MessagePortIpc
import org.dweb_browser.dwebview.ipcWeb.saveNative2JsIpcPort
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.SimpleCallback
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
                        resolve(await createProcess(`$env_script_url`,$metadata_json_str,$env_json_str,fetch_port,`$host`,`{"jsMicroModule":"${JsMicroModule.VERSION}.${JsMicroModule.PATCH}"}`))
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
    val ipc = MessagePortIpc.from(port2, remoteModule, IPC_ROLE.CLIENT)
    return ProcessHandler(info, ipc)
  }

  suspend fun createIpcFail(
    process_id: String,
    mmid: String,
    reason: String
  ) {
    dWebView.evaluateAsyncJavascriptCode(
      """createIpcFail(`$process_id`,`$mmid`,`${reason}`)"""
    )
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


  suspend fun destroy() {
    dWebView.destroy()
  }

  suspend fun onDestory(cb: SimpleCallback) = dWebView.onDestroy(cb)
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
  /// 确保API可用
  while (dWebView.evaluateAsyncJavascriptCode("typeof createProcess==='function'") == "false") {
    delay(5)
  }

  return JsProcessWebApi(dWebView)
}
