package info.bagen.dwebbrowser.microService.browser.jsProcess

import android.net.Uri
import android.webkit.WebMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.ipcWeb.MessagePortIpc
import org.dweb_browser.dwebview.ipcWeb.saveNative2JsIpcPort
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.help.MicroModuleManifest
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.ipc.helper.IPC_ROLE
import java.util.concurrent.atomic.AtomicInteger

class JsProcessWebApi(val dWebView: DWebView) {
  suspend fun isReady() =
    dWebView.evaluateSyncJavascriptCode("typeof createProcess") == "function"

  data class ProcessInfo(val process_id: Int) {}
  inner class ProcessHandler(val info: ProcessInfo, var ipc: MessagePortIpc)

  /**
   * 执行js"多步骤"代码时的并发编号
   */
  private var hidAcc = AtomicInteger(1);

  suspend fun createProcess(
    env_script_url: String,
    metadata_json: String,
    env_json: String,
    remoteModule: MicroModuleManifest,
    host: String
  ) = withContext(Dispatchers.Main) {
    val channel = dWebView.createWebMessageChannel()
    val port1 = channel[0]
    val port2 = channel[1]
    val metadata_json_str = gson.toJson(metadata_json)
    val env_json_str = gson.toJson(env_json)

    val hid = hidAcc.addAndGet(1);
    val processInfo_json = dWebView.evaluateAsyncJavascriptCode("""
            new Promise((resolve,reject)=>{
                addEventListener("message", async function doCreateProcess(event) {
                    if (event.data === "js-process/create-process/$hid") {
                     try{
                        removeEventListener("message", doCreateProcess);
                        const fetch_port = event.ports[0];
                        resolve(await createProcess(`$env_script_url`,$metadata_json_str,$env_json_str,fetch_port,`$host`))
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
    val info = gson.fromJson(processInfo_json, ProcessInfo::class.java)
    val ipc = MessagePortIpc.from(port2, remoteModule, IPC_ROLE.CLIENT)
    ProcessHandler(info, ipc)
  }

  suspend fun createIpcFail(
    process_id: String,
    mmid: String,
    reason: String
  ) = dWebView.evaluateAsyncJavascriptCode(
    """
        createIpcFail($process_id,$mmid,$reason)
        """.trimIndent()
  ).let {}

  data class RunProcessMainOptions(val main_url: String)

  suspend fun runProcessMain(process_id: Int, options: RunProcessMainOptions) =
    dWebView.evaluateAsyncJavascriptCode(
      """
        runProcessMain($process_id, { main_url:`${options.main_url}` })
        """.trimIndent()
    ).let {}

  suspend fun destroyProcess(process_id: Int) =
    dWebView.evaluateAsyncJavascriptCode(
      """
        destroyProcess($process_id)
        """.trimIndent()
    ).let {}

  suspend fun createIpc(process_id: Int, mmid: MMID) = withContext(Dispatchers.Main) {
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


  fun destroy() {
    dWebView.destroy()
  }
}