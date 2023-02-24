package info.bagen.rust.plaoc.microService.sys.js

import android.net.Uri
import android.webkit.WebMessage
import android.webkit.WebView
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.WebViewAsyncEvalContext
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.suspendOnce
import info.bagen.rust.plaoc.microService.helper.text
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcHeaders
import info.bagen.rust.plaoc.microService.ipc.IpcResponse
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.MessagePortIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.saveNative2JsIpcPort
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.http.DwebHttpServerOptions
import info.bagen.rust.plaoc.microService.sys.http.createHttpDwebServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class JsProcessNMM : NativeMicroModule("js.sys.dweb") {
    private var nww: WebView? = null
    override suspend fun _bootstrap() {

        /// 主页的网页服务
        val mainServer = this.createHttpDwebServer(DwebHttpServerOptions()).also { server ->
            // 在模块关停的时候，要关闭端口监听
            _afterShutdownSignal.listen { server.close() }
            // 提供基本的主页服务
            val serverIpc = server.listen();
            serverIpc.onRequest { (request, ipc) ->
                ipc.postMessage(
                    IpcResponse.fromResponse(
                        request.req_id,
                        nativeFetch("file:///bundle/js-process${request.uri.path}"),
                        ipc
                    )
                )
            }
        }

        println("mainServer: ${mainServer.startResult}")

        /// WebWorker的环境服务
        val internalServer =
            this.createHttpDwebServer(DwebHttpServerOptions(subdomain = "internal"))
                .also { server ->
                    // 在模块关停的时候，要关闭端口监听
                    _afterShutdownSignal.listen { server.close() }
                    val JS_PROCESS_WORKER_CODE =
                        suspendOnce { nativeFetch("file:///bundle/js-process.worker.js").text() }
                    // 提供基本的主页服务
                    val serverIpc = server.listen()
                    serverIpc.onRequest { (request, ipc) ->
                        if (request.uri.path === "/bootstrap.js") {
                            ipc.postMessage(
                                IpcResponse.fromText(
                                    request.req_id,
                                    200,
                                    IpcHeaders().also {
                                        it.set(
                                            "content-type",
                                            "application/javascript"
                                        )
                                    },
                                    JS_PROCESS_WORKER_CODE(),
                                    ipc
                                )
                            )
                        }
                    }
                }

        println("internalServer: ${internalServer.startResult}")


        /// WebView 实例
        val apis = withContext(Dispatchers.Main) {
            WebView.setWebContentsDebuggingEnabled(true)
            val webView = WebView(App.appContext).also {
                nww = it
                val urlInfo = mainServer.startResult.urlInfo
                it.settings.userAgentString += " dweb-host/${urlInfo.host}"
                it.settings.javaScriptEnabled = true
                it.settings.domStorageEnabled = true
                it.settings.databaseEnabled = true
                it.loadUrl(urlInfo.public_origin + "/index.html")
            }
            JsProcessWebApi(webView)
        }

        val query_main_pathname = Query.string().required("main_pathname")
        val query_process_id = Query.int().required("process_id")

        apiRouting = routes(
            /// 创建 web worker
            "/create-process" bind Method.POST to defineHandler { request, ipc ->
                createProcessAndRun(
                    ipc,
                    apis,
                    "${internalServer.startResult.urlInfo.internal_origin}/bootstrap.js?mmid=${ipc.remote.mmid}",
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
    ): Response {

        /**
         * 用自己的域名的权限为它创建一个子域名
         */
        val httpDwebServer = createHttpDwebServer(
            DwebHttpServerOptions(subdomain = ipc.remote.mmid),
        );

        /**
         * 远端是代码服务，所以这里是 client 的身份
         */
        val streamIpc = ReadableStreamIpc(ipc.remote, IPC_ROLE.CLIENT).also {
            it.bindIncomeStream(requestMessage.body.stream, "code-proxy-server");
        }

        /**
         * 代理监听
         * 让远端提供 esm 模块代码
         * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
         * 我们会对回来的代码进行处理，然后再执行
         */
        val codeProxyServerIpc = httpDwebServer.listen()
        codeProxyServerIpc.onRequest { (request, ipc) ->
            // TODO 对代码进行翻译处理
            // 转发给远端来处理
            ipc.responseBy(streamIpc, request)
        }


        /**
         * 开始执行代码
         */
        val processHandler = apis.createProcess(bootstrap_url, ipc.remote);

        /// 绑定销毁

        /**
         * “模块之间的IPC通道”关闭的时候，关闭“代码IPC流通道”
         *
         * > 自己shutdown的时候，这些ipc会被关闭
         */
        ipc.onClose { streamIpc.close() }

        /**
         * “代码IPC流通道”关闭的时候，关闭这个子域名
         */
        streamIpc.onClose {
            httpDwebServer.close();
        }

        /// 返回自定义的 Response，里头携带我们定义的 ipcStream
        return Response(Status.OK).body(streamIpc.stream);
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
