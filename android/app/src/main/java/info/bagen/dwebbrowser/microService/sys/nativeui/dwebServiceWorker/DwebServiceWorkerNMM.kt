package info.bagen.dwebbrowser.microService.sys.nativeui.dwebServiceWorker

import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.helper.printdebugln
import info.bagen.dwebbrowser.microService.helper.runBlockingCatching
import info.bagen.dwebbrowser.microService.sys.dns.nativeFetch
import info.bagen.dwebbrowser.microService.sys.jmm.JmmNMM.Companion.getBfsMetaData
import info.bagen.dwebbrowser.microService.sys.jmm.JsMicroModule
import info.bagen.dwebbrowser.microService.sys.mwebview.MultiWebViewNMM.Companion.getCurrentWebViewController
import kotlinx.coroutines.*
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugDwebServiceWorker(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("DwebServiceWorker", tag, msg, err)

class DwebServiceWorkerNMM : NativeMicroModule("service-worker.nativeui.sys.dweb") {

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
//            "/close" bind Method.GET to defineHandler { request, ipc ->
//                val controller = getCurrentWebViewController(ipc.remote.mmid)
//                debugDwebServiceWorker("close", controller)
//                if (controller !== null) {
//                    controller.activity?.finish()
//                    controller.destroyWebView()
//                    return@defineHandler true
//                }
//                Response(Status.INTERNAL_SERVER_ERROR).body("not found WebView Controller!")
//            },
            "/restart" bind Method.GET to defineHandler { request, ipc ->
                // 关闭后端连接
                nativeFetch("file://dns.sys.dweb/close?app_id=${ipc.remote.mmid}")
                // 调用重启
                runBlockingCatching(ioAsyncExceptionHandler) {
                    // TODO 神奇的操作
                    delay(200)
                    bootstrapContext.dns.bootstrap(ipc.remote.mmid)
                }
                return@defineHandler true
            }
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}
