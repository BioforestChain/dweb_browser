package info.bagen.rust.plaoc.microService.sys.js

import android.net.Uri
import android.webkit.WebMessage
import android.webkit.WebView
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.WebViewAsyncEvalContext
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.MessagePortIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.saveNative2JsIpcPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JsProcessWebApi(val webView: WebView) {

    val asyncEvalContext = WebViewAsyncEvalContext(webView)

    suspend fun isReady() =
        asyncEvalContext.evaluateSyncJavascriptCode("typeof createProcess") == "function"

    data class ProcessInfo(val process_id: Int) {}
    inner class ProcessHandler(val info: ProcessInfo, var ipc: MessagePortIpc)

    suspend fun createProcess(env_script_url: String, remoteModule: MicroModule) =
        withContext(Dispatchers.Main) {
            val channel = webView.createWebMessageChannel()
            val port1 = channel[0]
            val port2 = channel[1]

            val processInfo_json = asyncEvalContext.evaluateAsyncJavascriptCode(
                """
            new Promise((resolve,reject)=>{
                addEventListener("message", async event => {
                    if (event.data === "js-process/create-process") {
                        const fetch_port = event.ports[0];
//                        await new Promise((resolve)=>{self.createProcess_start = resolve})
                        try{
                            resolve(await createProcess(`$env_script_url`, fetch_port))
                        }catch(err){
                            reject(err)
                        }
                    }
                }, { once: true })
            })
            """.trimIndent(),
                afterEval = {
                    webView.postWebMessage(
                        WebMessage("js-process/create-process", arrayOf(port1)),
                        Uri.EMPTY
                    );
                }
            )
            debugJsProcess("processInfo", processInfo_json)
            val info = gson.fromJson(processInfo_json, ProcessInfo::class.java)
            ProcessHandler(info, MessagePortIpc(port2, remoteModule, IPC_ROLE.CLIENT))
        }


    data class RunProcessMainOptions(val main_url: String)

    suspend fun runProcessMain(process_id: Int, options: RunProcessMainOptions) =
        asyncEvalContext.evaluateAsyncJavascriptCode(
            """
        runProcessMain($process_id, { main_url:`${options.main_url}` })
        """.trimIndent()
        ).let {}

    suspend fun createIpc(process_id: Int) = asyncEvalContext.evaluateAsyncJavascriptCode(
        """
        new Promise((resolve,reject)=>{
            addEventListener("message", async event => {
                if (event.data === "js-process/create-ipc") {
                    const ipc_port = event.port[0];
                    try{
                        resolve(await createIpc($process_id, ipc_port))
                    }catch(err){
                        reject(err)
                    }
                }
            }, { once: true })
        })
        """.trimIndent()
    ).let {
        val channel = webView.createWebMessageChannel()
        val port1 = channel[0]
        val port2 = channel[1]
        webView.postWebMessage(
            WebMessage("js-process/create-ipc", arrayOf(port1)),
            android.net.Uri.EMPTY
        );

        saveNative2JsIpcPort(port2)
    }
}