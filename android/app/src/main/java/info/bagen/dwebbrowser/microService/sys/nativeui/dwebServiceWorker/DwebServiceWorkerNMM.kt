package info.bagen.dwebbrowser.microService.sys.nativeui.dwebServiceWorker

import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.*
import info.bagen.dwebbrowser.microService.sys.dns.nativeFetch
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
            // 提供给应用自卸载
            "/uninstall" bind Method.GET to defineHandler { request, ipc ->
                debugDwebServiceWorker("uninstall", "应用触发自卸载")
                Response(Status.INTERNAL_SERVER_ERROR).body("error for remove app!")
            },
            "/restart" bind Method.GET to defineHandler { request, ipc ->
                // 关闭后端连接
                nativeFetch("file://dns.sys.dweb/close?app_id=${ipc.remote.mmid}")
                // 调用重启
                runBlockingCatching(ioAsyncExceptionHandler) {
                    // TODO 神奇的操作
                    delay(200)
                    bootstrapContext.dns.bootstrap(ipc.remote.mmid)
                }
            },
            "emitUpdateFoundEvent"  bind Method.GET to defineHandler { request, ipc ->
                debugDwebServiceWorker("emitUpdateFoundEvent",ipc.remote.mmid)
                // 触发UpdateFound 事件
               return@defineHandler emitEvent(ipc.remote.mmid,ServiceWorkerEvent.UpdateFound.event)
            }
        )
    }

    override suspend fun _shutdown() {
    }
}
