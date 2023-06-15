package info.bagen.dwebbrowser.microService.browser.jsProcess

import android.webkit.WebView
import info.bagen.dwebbrowser.App
import org.dweb_browser.microservice.sys.dns.nativeFetch
import info.bagen.dwebbrowser.microService.browser.webview.DWebView
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.message.IpcHeaders
import org.dweb_browser.microservice.ipc.message.IpcResponse
import org.dweb_browser.microservice.ipc.ReadableStreamIpc
import org.dweb_browser.microservice.help.Mmid
import org.dweb_browser.helper.encodeURI
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.sys.http.DwebHttpServerOptions
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.microservice.sys.http.closeHttpDwebServer
import org.dweb_browser.microservice.sys.http.createHttpDwebServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.help.text
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugJsProcess(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("js-process", tag, msg, err)


class JsProcessNMM : NativeMicroModule("js.browser.dweb") {

    private val JS_PROCESS_WORKER_CODE by lazy {
        runBlockingCatching {
          nativeFetch("file:///sys/browser/js-process/worker-thread/js-process.worker.js").text()
        }.getOrThrow()
    }

    private val CORS_HEADERS = mapOf(
        Pair("Content-Type", "text/javascript"),
        Pair("Access-Control-Allow-Origin", "*"),
        Pair("Access-Control-Allow-Headers", "*"),// 要支持 X-Dweb-Host
        Pair("Access-Control-Allow-Methods", "*"),
    )

    private val INTERNAL_PATH = "/<internal>".encodeURI()


    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        /// 主页的网页服务
        val mainServer = this.createHttpDwebServer(DwebHttpServerOptions()).also { server ->
            // 提供基本的主页服务
            val serverIpc = server.listen();
            serverIpc.onRequest { (request, ipc) ->
                // <internal>开头的是特殊路径，给Worker用的，不会拿去请求文件
                if (request.uri.path.startsWith(INTERNAL_PATH)) {
                    val internalUri =
                        request.uri.path(request.uri.path.substring(INTERNAL_PATH.length));
                    if (internalUri.path == "/bootstrap.js") {
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
                    val response = nativeFetch("file:///sys/browser/js-process/main-thread${request.uri.path}")
                    ipc.postMessage(
                        IpcResponse.fromResponse(request.req_id, response, ipc)
                    )
                }
            }
        }
        val bootstrap_url =
            mainServer.startResult.urlInfo.buildInternalUrl().path("$INTERNAL_PATH/bootstrap.js")
                .toString()

        val apis = createJsProcessWeb(mainServer)
        val query_entry = Query.string().optional("entry")
        val query_process_id = Query.string().required("process_id")
        val query_mmid = Query.string().required("mmid")

        val ipcProcessIdMap = mutableMapOf<Ipc, MutableMap<String, PromiseOut<Int>>>()
        val processIpcMap = mutableMapOf<String, Ipc>()
        val ipcProcessIdMapLock = Mutex()
        apiRouting = routes(
            /// 创建 web worker
            // request 需要携带一个流，来为 web worker 提供代码服务
            "/create-process" bind Method.POST to defineHandler { request, ipc ->
                processIpcMap[ipc.remote.mmid] = ipc
                val po = ipcProcessIdMapLock.withLock {
                    val processId = query_process_id(request)
                    val processIdMap = ipcProcessIdMap.getOrPut(ipc) {
                        ipc.onClose { ipcProcessIdMap.remove(ipc) }
                        mutableMapOf()
                    }

                    if (processIdMap.contains(processId)) {
                        throw Exception("ipc:${ipc.remote.mmid}/processId:$processId has already using")
                    }

                    PromiseOut<Int>().also { processIdMap[processId] = it }
                }
                val result = createProcessAndRun(
                    ipc, apis,
                    bootstrap_url,
                    query_entry(request), request,
                )
                // 将自定义的 processId 与真实的 js-process_id 进行关联
                po.resolve(result.processHandler.info.process_id)

                // 返回流，因为构建了一个双工通讯用于代码提供服务
                result.streamIpc.stream
            },
            /// 创建 web 通讯管道
            "/create-ipc" bind Method.GET to defineHandler { request, ipc ->
                val processId = query_process_id(request)

                /**
                 * 虽然 mmid 是从远程直接传来的，但风险与jsProcess无关，
                 * 因为首先我们是基于 ipc 来得到 processId 的，所以这个 mmid 属于 ipc 自己的定义
                 */
                val mmid = query_mmid(request)
                val process_id = ipcProcessIdMapLock.withLock {
                    ipcProcessIdMap[ipc]?.get(processId)
                        ?: throw Exception("ipc:${ipc.remote.mmid}/processId:$processId invalid")
                }.waitPromise()

                // 返回 port_id
                createIpc(ipc, apis, process_id, mmid)
            },
            /// 关闭process
            "/close-process" bind Method.GET to defineHandler { request,ipc ->
                closeHttpDwebServer(DwebHttpServerOptions(port = 80,subdomain = ipc.remote.mmid))
                val processIpc = processIpcMap[ipc.remote.mmid]
                debugJsProcess("close-process",processIpc?.remote?.mmid)
                processIpc?.close()
                return@defineHandler true
            }
        )
    }


    private suspend fun createJsProcessWeb(mainServer: HttpDwebServer): JsProcessWebApi {
        val afterReadyPo = PromiseOut<Unit>()
        /// WebView 实例
        val apis = withContext(Dispatchers.Main) {
            WebView.setWebContentsDebuggingEnabled(true)

            val urlInfo = mainServer.startResult.urlInfo
            JsProcessWebApi(
                DWebView(
                    App.appContext, this@JsProcessNMM, this@JsProcessNMM, DWebView.Options(
                        url = urlInfo.buildInternalUrl().path("/index.html").toString()
                    )
                )
            ).also { api ->
                api.dWebView.onReady { afterReadyPo.resolve(Unit) }
            }
        }
        afterReadyPo.waitPromise()
        return apis
    }

    override suspend fun _shutdown() {
    }

    private suspend fun createProcessAndRun(
      ipc: Ipc,
      apis: JsProcessWebApi,
      bootstrap_url: String,
      entry: String?,
      requestMessage: Request,
    ): CreateProcessAndRunResult {
        /**
         * 用自己的域名的权限为它创建一个子域名
         */
        val httpDwebServer = createHttpDwebServer(
            DwebHttpServerOptions(subdomain = ipc.remote.mmid),
        );

        /**
         * 远端是代码服务，所以这里是 client 的身份
         */
        val streamIpc = ReadableStreamIpc(ipc.remote, "code-proxy-server").also {
            it.bindIncomeStream(requestMessage.body.stream);
        }

        /**
         * 代理监听
         * 让远端提供 esm 模块代码
         * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
         * 我们会对回来的代码进行处理，然后再执行
         */
        val codeProxyServerIpc = httpDwebServer.listen()

        codeProxyServerIpc.onRequest { (request, ipc) ->
            ipc.postResponse(
                request.req_id,
                // 转发给远端来处理
                /// TODO 对代码进行翻译处理
                streamIpc.request(request.toRequest()).let {
                    /// 加入跨域配置
                    var response = it;
                    for ((key, value) in CORS_HEADERS) {
                        response = response.header(key, value)
                    }
                    response
                },
            )
        }

        data class JsProcessMetadata(val mmid: Mmid) {}
        /// TODO 需要传过来，而不是自己构建
        val metadata = JsProcessMetadata(ipc.remote.mmid)

        /// TODO env 允许远端传过来扩展
        val env = mutableMapOf( // ...your envs
            Pair("host", httpDwebServer.startResult.urlInfo.host),
            Pair("debug", "true"),
            Pair("ipc-support-protocols", "")
        )

        /**
         * 创建一个通往 worker 的消息通道
         */
        val processHandler = apis.createProcess(
            bootstrap_url,
            gson.toJson(metadata),
            gson.toJson(env),
            ipc.remote,
            httpDwebServer.startResult.urlInfo.host
        );
        /**
         * 收到 Worker 的数据请求，由 js-process 代理转发回去，然后将返回的内容再代理响应会去
         *
         * TODO 所有的 ipcMessage 应该都有 headers，这样我们在 workerIpcMessage.headers 中附带上当前的 processId，回来的 remoteIpcMessage.headers 同样如此，否则目前的模式只能代理一个 js-process 的消息。另外开 streamIpc 导致的翻译成本是完全没必要的
         */
        processHandler.ipc.onMessage { (workerIpcMessage) ->
            /**
             * 直接转发给远端 ipc，如果是nativeIpc，那么几乎没有性能损耗
             */
            ipc.postMessage(workerIpcMessage)
        }
        ipc.onMessage { (remoteIpcMessage) ->
            processHandler.ipc.postMessage(remoteIpcMessage)
        }

        /**
         * 开始执行代码
         */
        apis.runProcessMain(
            processHandler.info.process_id, JsProcessWebApi.RunProcessMainOptions(
                main_url = httpDwebServer.startResult.urlInfo.buildInternalUrl()
                    .path(entry ?: "/index.js").toString()
            )
        )

        /// 绑定销毁
        /**
         * “模块之间的IPC通道”关闭的时候，关闭“代码IPC流通道”
         *
         * > 自己shutdown的时候，这些ipc会被关闭
         */
        ipc.onClose {
            streamIpc.close()
            codeProxyServerIpc.close()
        }

        /**
         * “代码IPC流通道”关闭的时候，关闭这个子域名
         */
        streamIpc.onClose {
            httpDwebServer.close();
            apis.destroyProcess(processHandler.info.process_id)
        }

        return CreateProcessAndRunResult(streamIpc, processHandler)
    }

    data class CreateProcessAndRunResult(
      val streamIpc: ReadableStreamIpc, val processHandler: JsProcessWebApi.ProcessHandler
    )

    private suspend fun createIpc(
      ipc: Ipc, apis: JsProcessWebApi, process_id: Int, mmid: Mmid
    ): Int {
        return apis.createIpc(process_id, mmid)
    }
}


