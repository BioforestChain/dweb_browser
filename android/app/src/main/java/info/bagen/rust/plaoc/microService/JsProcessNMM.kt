package info.bagen.rust.plaoc.microService

import android.webkit.WebView
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.helper.suspendOnce
import info.bagen.rust.plaoc.microService.helper.text
import info.bagen.rust.plaoc.microService.ipc.IpcHeaders
import info.bagen.rust.plaoc.microService.ipc.IpcResponse
import info.bagen.rust.plaoc.microService.network.nativeFetch
import org.http4k.core.Method
import org.http4k.lens.Query
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
                            IpcHeaders().also { it.set("content-type", "application/javascript") },
                            args.ipc
                        )
                    )
                }
            }
        }

        /// WebView 实例
        nww = WebView(App.appContext).also {
            it.loadUrl(mainServer.start().origin)
        }


        val  query_main_pathname = Query.string().required("main_pathname")

        apiRouting = routes(
            "/create-process" bind Method.POST to defineHandler {request->

            }
        )


    }

    override suspend fun _shutdown() {
        nww?.let {
            it.destroy()
            nww = null
        }
    }
}


//class ImportLinker(val origin: String, val importMaps: RoutingHttpHandler) {
//    fun link(request: Request): Response {
//        return importMaps(request)
//    }
//}