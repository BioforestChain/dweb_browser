package info.bagen.rust.plaoc.microService.sys.js

import android.net.Uri
import android.webkit.WebMessage
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.MessagePortIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.saveNative2JsIpcPort
import info.bagen.rust.plaoc.microService.webview.DWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JsProcessWebApi(val dWebView: DWebView) {
    suspend fun isReady() =
        dWebView.evaluateSyncJavascriptCode("typeof createProcess") == "function"

    data class ProcessInfo(val process_id: Int) {}
    inner class ProcessHandler(val info: ProcessInfo, var ipc: MessagePortIpc)

    suspend fun createProcess(
        env_script_url: String,
        metadata_json: String,
        env_json: String,
        remoteModule: Ipc.MicroModuleInfo,
        host: String
    ) = withContext(Dispatchers.Main) {
        val channel = dWebView.createWebMessageChannel()
        val port1 = channel[0]
        val port2 = channel[1]
        val metadata_json_str = gson.toJson(metadata_json)
        val env_json_str = gson.toJson(env_json)

        val processInfo_json = dWebView.evaluateAsyncJavascriptCode("""
            new Promise((resolve,reject)=>{
                addEventListener("message", async event => {
                    if (event.data === "js-process/create-process") {
                        const fetch_port = event.ports[0];
                        try{
                            resolve(await createProcess(`$env_script_url`,$metadata_json_str,$env_json_str,fetch_port,`$host`))
                        }catch(err){
                            reject(err)
                        }
                    }
                }, { once: true })
            })
            """.trimIndent(), afterEval = {
            try {

                dWebView.postWebMessage(
                    WebMessage("js-process/create-process", arrayOf(port1)), Uri.EMPTY
                );
            } catch (e: Exception) {
                println("QQQQ");
                e.printStackTrace()
            }
        })
        debugJsProcess("processInfo", processInfo_json)
        val info = gson.fromJson(processInfo_json, ProcessInfo::class.java)
        ProcessHandler(info, MessagePortIpc(port2, remoteModule, IPC_ROLE.CLIENT))
    }


    data class RunProcessMainOptions(val main_url: String)

    suspend fun runProcessMain(process_id: Int, options: RunProcessMainOptions) =
        dWebView.evaluateAsyncJavascriptCode(
            """
        runProcessMain($process_id, { main_url:`${options.main_url}` })
        """.trimIndent()
        ).let {}

    suspend fun createIpc(process_id: Int, mmid: Mmid) = withContext(Dispatchers.Main) {
        val channel = dWebView.createWebMessageChannel()
        val port1 = channel[0]
        val port2 = channel[1]
        dWebView.evaluateAsyncJavascriptCode("""
        new Promise((resolve,reject)=>{
            addEventListener("message", async event => {
                if (event.data === "js-process/create-ipc") {
                    const ipc_port = event.ports[0];
                    try{
                        resolve(await createIpc($process_id, `$mmid`, ipc_port))
                    }catch(err){
                        reject(err)
                    }
                }
            }, { once: true })
        })
        """.trimIndent(), afterEval = {
            dWebView.postWebMessage(
                WebMessage("js-process/create-ipc", arrayOf(port1)), Uri.EMPTY
            );
        })
        saveNative2JsIpcPort(port2)
    }

    fun destroy() {
        dWebView.destroy()
    }
}