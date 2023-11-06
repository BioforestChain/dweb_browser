package org.dweb_browser.browser.jsProcess

import android.net.Uri
import android.webkit.WebMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.dwebview.ipcWeb.MessagePortIpc
import org.dweb_browser.dwebview.ipcWeb.saveNative2JsIpcPort
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.ipc.helper.IPC_ROLE
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.build
import org.dweb_browser.helper.resolvePath
import java.util.concurrent.atomic.AtomicInteger

class JsProcessWebApi(val dWebView: DWebViewEngine) : IJsProcessWebApi {
  suspend fun isReady() =
    dWebView.evaluateSyncJavascriptCode("typeof createProcess") == "function"

  /**
   * 执行js"多步骤"代码时的并发编号
   */
  private var hidAcc = AtomicInteger(1);


  override suspend fun createProcess(
    env_script_url: String,
    metadata_json: String,
    env_json: String,
    remoteModule: IMicroModuleManifest,
    host: String
  ) = withContext(Dispatchers.Main) {
    val channel = dWebView.createWebMessageChannel()
    val port1 = channel[0]
    val port2 = channel[1]
    val metadata_json_str = Json.encodeToString(metadata_json)
    val env_json_str = Json.encodeToString(env_json)

    val hid = hidAcc.addAndGet(1);
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
        dWebView.postWebMessage(
          WebMessage("js-process/create-process/$hid", arrayOf(port1)), Uri.EMPTY
        );
      } catch (e: Exception) {
        e.printStackTrace()
      }
    })
    debugJsProcess("processInfo", processInfo_json)
    val info = Json.decodeFromString<ProcessInfo>(processInfo_json)
    val ipc = MessagePortIpc.from(port2, remoteModule, IPC_ROLE.CLIENT)
    ProcessHandler(info, ipc)
  }

  override suspend fun createIpcFail(
    process_id: String,
    mmid: String,
    reason: String
  ) = dWebView.evaluateAsyncJavascriptCode(
    """
        createIpcFail($process_id,$mmid,$reason)
        """.trimIndent()
  ).let {}

  override suspend fun runProcessMain(process_id: Int, options: RunProcessMainOptions) =
    dWebView.evaluateAsyncJavascriptCode(
      """
        runProcessMain($process_id, { main_url:`${options.main_url}` })
        """.trimIndent()
    ).let {}

  override suspend fun destroyProcess(process_id: Int) =
    dWebView.evaluateAsyncJavascriptCode(
      """
        destroyProcess($process_id)
        """.trimIndent()
    ).let {}

  override suspend fun createIpc(process_id: Int, mmid: MMID) = withContext(Dispatchers.Main) {
    val channel = dWebView.createWebMessageChannel()
    val port1 = channel[0]
    val port2 = channel[1]
    val jsIpcPortId = saveNative2JsIpcPort(port2)
    val hid = hidAcc.getAndAdd(1);
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
      dWebView.postWebMessage(
        WebMessage("js-process/create-ipc/$hid", arrayOf(port1)), Uri.EMPTY
      );
    })
    jsIpcPortId
  }


  override suspend fun destroy() {
    dWebView.destroy()
  }

  override suspend fun onDestory(cb: SimpleCallback) = dWebView.onDestroy(cb)
}

actual suspend fun createJsProcessWeb(mainServer: HttpDwebServer, mm: NativeMicroModule): IJsProcessWebApi {
  val afterReadyPo = PromiseOut<Unit>()
  /// WebView 实例
  val apis = withContext(Dispatchers.Main) {

    val urlInfo = mainServer.startResult.urlInfo
    JsProcessWebApi(DWebViewEngine(
      mm.getAppContext(), mm, DWebViewOptions(
        url = urlInfo.buildInternalUrl().build { resolvePath("/index.html") }.toString()
      )
    ).also { it.settings.safeBrowsingEnabled = false }).also { api ->
      api.dWebView.onReady { afterReadyPo.resolve(Unit) }
    }
  }
  afterReadyPo.waitPromise()
  return apis
}
