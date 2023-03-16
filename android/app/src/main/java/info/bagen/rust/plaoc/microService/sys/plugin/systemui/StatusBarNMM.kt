package info.bagen.rust.plaoc.microService.sys.plugin.systemui


import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.OffListener
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcEvent
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class StatusBarNMM : NativeMicroModule("status-bar.sys.dweb") {

    private fun getController(mmid: Mmid) =
        NativeUiController.fromMultiWebView(mmid).statusBar

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        QueryHelper.init()
        val observers = mutableMapOf<Ipc, OffListener>()
        apiRouting = routes(
            /** 获取状态栏信息*/
            "/getInfo" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler getController(ipc.remote.mmid)
            },
            /**
             * 开始数据订阅
             */
            "/startObserve" bind Method.GET to defineHandler { request, ipc ->
                val controller = getController(ipc.remote.mmid)
                if (!observers.containsKey(ipc)) {
                    observers[ipc] = controller.observe {
                        ipc.postMessage(
                            IpcEvent.fromUtf8(
                                "observe",
                                gson.toJson(controller.toJsonAble())
                            )
                        )
                    }
                }
                return@defineHandler getController(ipc.remote.mmid)
            },
            /**
             * 开始数据订阅
             */
            "/stopObserve" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler observers.remove(ipc)?.let { off ->
                    off(Unit)
                    true
                } ?: false
            },
            /** 设置状态栏背景色*/
            "/setBackgroundColor" bind Method.GET to defineHandler { request, ipc ->
                val color = QueryHelper.getColor(request)
                getController(ipc.remote.mmid).colorState.value = color
                return@defineHandler null
            },
            /** 设置状态栏风格*/
            "/setStyle" bind Method.GET to defineHandler { request, ipc ->
                val style = QueryHelper.style(request)
                getController(ipc.remote.mmid).styleState.value = style
                return@defineHandler null
            },
            /** 设置状态栏是否覆盖webview*/
            "/setOverlay" bind Method.GET to defineHandler { request, ipc ->
                val overlay = QueryHelper.overlay(request)
                getController(ipc.remote.mmid).overlayState.value = overlay
                return@defineHandler null
            },
            /** 设置状态栏是否可见 */
            "/setVisible" bind Method.GET to defineHandler { request, ipc ->
                val visible = QueryHelper.getVisible(request)
                getController(ipc.remote.mmid).visibleState.value = visible
                return@defineHandler null
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}
