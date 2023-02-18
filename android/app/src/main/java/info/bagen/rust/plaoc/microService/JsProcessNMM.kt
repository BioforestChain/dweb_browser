package info.bagen.rust.plaoc.microService

import android.net.Uri
import android.webkit.WebMessage
import android.webkit.WebView
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.helper.WebViewAsyncEvalContext
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.suspendOnce
import info.bagen.rust.plaoc.microService.helper.text
import info.bagen.rust.plaoc.microService.ipc.*
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.MessagePortIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.saveNative2JsIpcPort
import info.bagen.rust.plaoc.microService.network.nativeFetch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import okhttp3.internal.notify
import okhttp3.internal.wait
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class JsProcessNMM : NativeMicroModule("js.sys.dweb") {
    private var nww: WebView? = null
    override suspend fun _bootstrap() {

        /// 主页的网页服务
        val mainServer = this.createHttpDwebServer(DwebServerOptions()).also { server ->
            // 在模块关机的时候，要关闭端口监听
            _afterShutdownSignal.listen { server.close() }
            // 提供基本的主页服务
            server.listen().onRequest { args ->
                args.ipc.postMessage(
                    IpcResponse.fromResponse(
                        args.request.req_id,
                        nativeFetch("file:///bundle/js-process/${args.request.uri.path}"),
                        args.ipc
                    )
                )
            }
        }

        /// WebWorker的环境服务
        val internalServer =
            this.createHttpDwebServer(DwebServerOptions(subdomain = "internal")).also { server ->
                // 在模块关机的时候，要关闭端口监听
                _afterShutdownSignal.listen { server.close() }
                val JS_PROCESS_WORKER_CODE =
                    suspendOnce { nativeFetch("file:///bundle/js-process.worker.js").text() }
                // 提供基本的主页服务
                server.listen().onRequest { args ->
                    if (args.request.uri.path === "/bootstrap.js") {
                        args.ipc.postMessage(
                            IpcResponse.fromText(
                                args.request.req_id,
                                200, JS_PROCESS_WORKER_CODE(),
                                IpcHeaders().also {
                                    it.set(
                                        "content-type",
                                        "application/javascript"
                                    )
                                },
                                args.ipc
                            )
                        )
                    }
                }
            }
        val internalServerInfo = internalServer.start()

        /// WebView 实例
        val webView = WebView(App.appContext).also {
            nww = it
            it.loadUrl(mainServer.start().origin)
        }
        val apis = JsProcessWebApi(webView)

        val query_main_pathname = Query.string().required("main_pathname")
        val query_process_id = Query.int().required("process_id")

        apiRouting = routes(
            /// 创建 web worker
            "/create-process" bind Method.POST to defineHandler { request, ipc ->
                createProcessAndRun(
                    ipc, apis,
                    "${internalServerInfo.origin}/bootstrap.js?mmid=${ipc.remote.mmid}",
                    query_main_pathname(request),
                    request
                )
            },
            /// 创建 web 通讯管道
            "/create-ipc" bind Method.GET to defineHandler { request ->
                apis.createIpc(query_process_id(request))
            }
        )


    }

    override suspend fun _shutdown() {
        nww?.let {
            it.destroy()
            nww = null
        }
    }

    private suspend fun createProcessAndRun(
        ipc: Ipc,
        apis: JsProcessWebApi,
        bootstrap_url: String,
        main_pathname: String = "/index.js",
        requestMessage: Request
    ) {

        /**
         * 用自己的域名的权限为它创建一个子域名
         */
        val httpDwebServer = createHttpDwebServer(
            DwebServerOptions(subdomain = ipc.remote.mmid),
        );

        /**
         * 远端是代码服务，所以这里是 client 的身份
         */
        val streamIpc = ReadableStreamIpc(ipc.remote, IPC_ROLE.CLIENT).also {
            it.bindIncomeStream(requestMessage.body.stream);
        }

        httpDwebServer.listen().onRequest { args ->
            args.ipc.responseBy(streamIpc, args.request) /// 转发请求给listen的服务
        }
    }
}


class JsProcessWebApi(val webView: WebView) {

    val asyncEvalContext = WebViewAsyncEvalContext(webView)

    data class ProcessInfo(val process_id: Int) {}
    inner class ProcessHandler(val info: ProcessInfo, var ipc: MessagePortIpc)

    suspend fun createProcess(env_script_url: String, remoteModule: MicroModule) =
        asyncEvalContext.evaluateJavascriptAsync(
            """
        new Promise((resolve,reject)=>{
            addEventListener("message", async event => {
                if (event.data === "js-process/create-process") {
                    const fetch_port = event.port[0];
                    try{
                        resolve(await createProcess(`$env_script_url`, fetch_port))
                    }catch(err){
                        reject(err)
                    }
                }
            }, { once: true })
        });
        """.trimIndent()
        ).let {
            val info = gson.fromJson(it, ProcessInfo::class.java)

            val channel = webView.createWebMessageChannel()
            val port1 = channel[0]
            val port2 = channel[0]
            webView.postWebMessage(
                WebMessage("js-process/create-process", arrayOf(port1)),
                Uri.EMPTY
            );

            ProcessHandler(info, MessagePortIpc(port2, remoteModule, IPC_ROLE.CLIENT))
        }


    suspend fun runProcessMain(process_id: Int, main_url: String) =
        asyncEvalContext.evaluateJavascriptAsync(
            """
        runProcessMain($process_id, { main_url:$main_url s})
        """.trimIndent()
        ).let {}

    suspend fun createIpc(process_id: Int) = asyncEvalContext.evaluateJavascriptAsync(
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
        });
        """.trimIndent()
    ).let {
        val channel = webView.createWebMessageChannel()
        val port1 = channel[0]
        val port2 = channel[0]
        webView.postWebMessage(WebMessage("js-process/create-ipc", arrayOf(port1)), Uri.EMPTY);

        saveNative2JsIpcPort(port2)
    }
}