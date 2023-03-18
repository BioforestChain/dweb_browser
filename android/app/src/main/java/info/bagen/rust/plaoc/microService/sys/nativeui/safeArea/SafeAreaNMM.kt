package info.bagen.rust.plaoc.microService.sys.nativeui.safeArea

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.sys.nativeui.NativeUiController
import info.bagen.rust.plaoc.microService.sys.nativeui.fromMultiWebView
import info.bagen.rust.plaoc.microService.sys.nativeui.helper.QueryHelper
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class SafeAreaNMM : NativeMicroModule("safe-area.nativeui.sys.dweb") {

    private fun getController(mmid: Mmid) = NativeUiController.fromMultiWebView(mmid).safeArea

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            /** 获取状态 */
            "/getState" bind Method.GET to defineHandler { _, ipc ->
                return@defineHandler getController(ipc.remote.mmid)
            },
            /** 获取状态 */
            "/setState" bind Method.GET to defineHandler { request, ipc ->
                val controller = getController(ipc.remote.mmid)
                QueryHelper.overlay(request)?.also { controller.overlayState.value = it }
                return@defineHandler null
            },
            /**
             * 开始数据订阅
             */
            "/startObserve" bind Method.GET to defineHandler { _, ipc ->
                return@defineHandler getController(ipc.remote.mmid).observer.startObserve(ipc)
            },
            /**
             * 开始数据订阅
             */
            "/stopObserve" bind Method.GET to defineHandler { _, ipc ->
                return@defineHandler getController(ipc.remote.mmid).observer.stopObserve(ipc)
            },
        )
    }

    override suspend fun _shutdown() {
    }
}
