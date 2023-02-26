package info.bagen.rust.plaoc.microService.sys.js

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcHeaders
import info.bagen.rust.plaoc.microService.ipc.IpcResponse
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.http.DwebHttpServerOptions
import info.bagen.rust.plaoc.microService.sys.http.createHttpDwebServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.http4k.core.*
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes


inline fun debugJsProcess(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("js-process", tag, msg, err)


class JsProcessNMM : NativeMicroModule("js.sys.dweb") {

    private val JS_PROCESS_WORKER_CODE by lazy {
        runBlocking {
            nativeFetch("file:///bundle/js-process.worker.js").text()
        }
    }

    private val CORS_HEADERS = mapOf(
        Pair("Content-Type", "application/javascript"),
        Pair("Access-Control-Allow-Origin", "*"),
        Pair("Access-Control-Allow-Headers", "*"),// 要支持 X-Dweb-Host
        Pair("Access-Control-Allow-Methods", "*"),
    )

    private val INTERNAL_PATH = "/<internal>".encodeURI()


    override suspend fun _bootstrap() {

        /// 主页的网页服务
        val mainServer = this.createHttpDwebServer(DwebHttpServerOptions()).also { server ->
            // 在模块关停的时候，要关闭端口监听
            _afterShutdownSignal.listen { server.close() }
            // 提供基本的主页服务
            val serverIpc = server.listen();
            serverIpc.onRequest { (request, ipc) ->
                val response = nativeFetch("file:///bundle/js-process${request.uri.path}")
                ipc.postMessage(
                    IpcResponse.fromResponse(request.req_id, response, ipc)
                )
            }
        }

        println("mainServer: ${mainServer.startResult}")


        /// WebView 实例
        val apis = withContext(Dispatchers.Main) {
            WebView.setWebContentsDebuggingEnabled(true)

            JsProcessWebApi(WebView(App.appContext)).also { api ->
                val webView = api.webView
                val urlInfo = mainServer.startResult.urlInfo
                /// 注册销毁
                _afterShutdownSignal.listen {
                    webView.destroy()
                }
                webView.settings.userAgentString += " dweb-host/${urlInfo.host}"
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.databaseEnabled = true
                val isReady = PromiseOut<Unit>()
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isReady.resolve(Unit)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        if (request.method == "GET" && request.url.host?.endsWith(".dweb") == true && request.url.scheme == "http") {
                            val response = runBlocking {
                                nativeFetch(
                                    Request(
                                        Method.GET,
                                        request.url.toString()
                                    ).headers(request.requestHeaders.toList())
                                )
                            }
                            return WebResourceResponse(
                                response.header("Content-Type") ?: "application/octet-stream",
                                response.header("Content-Encoding") ?: "",
                                response.status.code,
                                response.status.description,
                                response.headers.toMap(),
                                response.body.stream
                            )
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
                /// 开始加载
                webView.loadUrl(urlInfo.buildInternalUrl().path("/index.html").toString())
                // 等待加载完成
                isReady.waitPromise()
            }
        }

        val query_main_pathname = Query.string().required("main_pathname")
        val query_process_id = Query.int().required("process_id")

        apiRouting = routes(
            /// 创建 web worker
            "/create-process" bind Method.POST to defineHandler { request, ipc ->
                createProcessAndRun(
                    ipc,
                    apis,
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

    }

    private suspend fun createProcessAndRun(
        ipc: Ipc,
        apis: JsProcessWebApi,
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
            // <internal>开头的是特殊路径：交由内部处理，不会推给远端处理
            if (request.uri.path.startsWith(INTERNAL_PATH)) {
                val internalUri =
                    request.uri.path(request.uri.path.substring(INTERNAL_PATH.length));
                if (internalUri.path == "bootstrap.js") {
                    ipc.postMessage(
                        IpcResponse.fromText(
                            request.req_id,
                            200,
                            IpcHeaders(CORS_HEADERS.toMutableMap()),
                            JS_PROCESS_WORKER_CODE,
                            ipc
                        )
                    )
                } else {
                    ipc.postMessage(
                        IpcResponse.fromText(
                            request.req_id,
                            404,
                            IpcHeaders(CORS_HEADERS.toMutableMap()),
                            "// no found ${internalUri.path}",
                            ipc
                        )
                    )
                }
            } else {
                ipc.postResponse(
                    request.req_id,
                    // 转发给远端来处理
                    // TODO 对代码进行翻译处理
                    streamIpc.request(request.asRequest()).let {
                        /// 加入跨域配置
                        var response = it;
                        for ((key, value) in CORS_HEADERS) {
                            response = response.header(key, value)
                        }
                        response
                    },
                )
            }
        }

        val bootstrap_url =
            httpDwebServer.startResult.urlInfo.buildInternalUrl().path("$INTERNAL_PATH/bootstrap.js")
                .query("mmid", ipc.remote.mmid).toString()

        /**
         * 创建一个通往 worker 的消息通道
         */
        val processHandler = apis.createProcess(bootstrap_url, ipc.remote);

        /// 收到 Worker 的数据请求，由 js-process 代理转发出去，然后将返回的内容再代理响应会去
        processHandler.ipc.onRequest { (request, ipc) ->
            val response = ipc.remote.nativeFetch(request.asRequest());
            ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
        }
        /**
         * 开始执行代码
         */
        apis.runProcessMain(
            processHandler.info.process_id, JsProcessWebApi.RunProcessMainOptions(
                main_url = httpDwebServer.startResult.urlInfo.buildInternalUrl()
                    .path(main_pathname)
                    .toString()
            )
        )

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


