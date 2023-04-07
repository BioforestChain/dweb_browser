package info.bagen.rust.plaoc.microService.sys.nativeui.dwebServiceWorker

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.http.DwebHttpServerOptions
import info.bagen.rust.plaoc.microService.sys.http.closeHttpDwebServer
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM.Companion.getAndUpdateJmmNmmApps
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM.Companion.getBfsMetaData
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM.Companion.getCurrentWebViewController
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugDwebServiceWorker(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("DwebServiceWorker", tag, msg, err)

class DwebServiceWorkerNMM:NativeMicroModule("service-worker.nativeui.sys.dweb") {

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            "/close" bind Method.GET to defineHandler { request,ipc ->
               // 后端已经在worker关闭，现在需要关闭前端
                val controller = getCurrentWebViewController(ipc.remote.mmid)
                debugDwebServiceWorker("close",controller)
                if (controller !== null) {
                    controller.activity?.finish()
                    controller.destroyWebView()
                    return@defineHandler true
                }
              Response(Status.INTERNAL_SERVER_ERROR).body("not found WebView Controller!")
            },
            "/restart" bind Method.GET to defineHandler { request, ipc ->
                val controller = getCurrentWebViewController(ipc.remote.mmid)
                debugDwebServiceWorker("restart",controller)
                if (controller == null) {
                    return@defineHandler Response(Status.INTERNAL_SERVER_ERROR).body("not found WebView Controller!")
                }
                // 关闭前端
                controller.activity?.finish()
                controller.destroyWebView()

                // 调用重启
                // TODO
                //  val jsMetadata = getBfsMetaData(ipc.remote.mmid)
//                debugDwebServiceWorker("restart remote mmid =>","${ipc.remote.mmid}  jsModule: ${jsMetadata?.id}")
//                if (jsMetadata == null) {
//                    return@defineHandler Response(Status.NOT_FOUND).body("not found the ${ipc.remote.mmid} js module !!")
//                }
                // 重新创建一个 jsModule
//                val jsModule = JsMicroModule(jsMetadata)
//                bootstrapContext.dns.bootstrap(jsModule.mmid)
//                jsModule.bootstrap(bootstrapContext)
//                nativeFetch("file://js.sys.dweb/close-process")
                return@defineHandler  true
            }
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}