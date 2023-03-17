package info.bagen.rust.plaoc.microService.sys.plugin.systemui

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class NavigationBarNMM : NativeMicroModule("navigation-bar.sys.dweb") {


    private fun getController(mmid: Mmid) =
        NativeUiController.fromMultiWebView(mmid).navigationBar

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        QueryHelper.init() // 初始化
        apiRouting = routes(
            /**
             * 设置导航栏
             */
            "/setState" bind Method.GET to defineHandler { request, ipc ->
                val controller = getController(ipc.remote.mmid)
                QueryHelper.color(request)?.also { controller.colorState.value = it }
                QueryHelper.style(request)?.also { controller.styleState.value = it }
                QueryHelper.overlay(request)?.also { controller.overlayState.value = it }
                QueryHelper.visible(request)?.also { controller.visibleState.value = it }
                return@defineHandler null
            },
            /**
             * 获取导航栏
             */
            "/getState" bind Method.GET to defineHandler { _, ipc ->
                return@defineHandler getController(ipc.remote.mmid)
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
        TODO("Not yet implemented")
    }
}
