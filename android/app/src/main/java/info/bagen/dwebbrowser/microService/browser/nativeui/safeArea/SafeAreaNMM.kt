package info.bagen.dwebbrowser.microService.browser.nativeui.safeArea

import org.dweb_browser.helper.MMID
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.fromMultiWebView
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.QueryHelper
import org.dweb_browser.helper.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

class SafeAreaNMM : NativeMicroModule("safe-area.nativeui.browser.dweb","safeArea") {

    override val categories = mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);

    private fun getController(mmid: MMID) = NativeUiController.fromMultiWebView(mmid).safeArea

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
            "/observe" bind Method.GET to defineHandler { _, ipc ->
                val inputStream = getController(ipc.remote.mmid).observer.startObserve(ipc)
                return@defineHandler Response(Status.OK).body(inputStream)
            },
            /**
             * 关闭订阅数据流
             */
            "/stopObserve" bind Method.GET to defineHandler { _, ipc ->
                return@defineHandler getController(ipc.remote.mmid).observer.stopObserve(ipc)
            },
        )
    }

    override suspend fun _shutdown() {
    }
}
