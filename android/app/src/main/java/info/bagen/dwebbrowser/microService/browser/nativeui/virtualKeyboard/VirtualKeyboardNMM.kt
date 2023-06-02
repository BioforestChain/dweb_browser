package info.bagen.dwebbrowser.microService.browser.nativeui.virtualKeyboard

import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.fromMultiWebView
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.QueryHelper
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.debugNativeUi
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class VirtualKeyboardNMM : NativeMicroModule("virtual-keyboard.nativeui.browser.dweb") {

    private fun getController(mmid: Mmid) =
        NativeUiController.fromMultiWebView(mmid).virtualKeyboard
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        QueryHelper.init()
        apiRouting = routes(
            /** 获取状态 */
            "/getState" bind Method.GET to defineHandler { _, ipc ->
                val controller = getController(ipc.remote.mmid);
                debugNativeUi("virtual-keyboard getState",controller.overlayState.value)
                return@defineHandler controller
            },
            /** 设置状态 */
            "/setState" bind Method.GET to defineHandler { request, ipc ->
                val controller = getController(ipc.remote.mmid)
                QueryHelper.overlay(request)?.also { controller.overlayState.value = it }
                QueryHelper.visible(request)?.also { controller.visibleState.value = it }
                return@defineHandler null
            },
            /**
             * 开始数据订阅
             */
            "/startObserve" bind Method.GET to defineHandler { _, ipc ->
                return@defineHandler getController(ipc.remote.mmid).observer.startObserve(ipc)
            },
            /**
             * 停止数据订阅
             */
            "/stopObserve" bind Method.GET to defineHandler { _, ipc ->
                return@defineHandler getController(ipc.remote.mmid).observer.stopObserve(ipc)
            },
        )
    }
//    override suspend fun observerState(event: IpcEvent, ipc: Ipc) {
//        super.observerState(event, ipc)
//        debugNativeUi("virtual-keyboard observerState","name:${event.name} mmid:${ipc.remote.mmid}")
//    }

    override suspend fun _shutdown() {
    }
}
