package info.bagen.rust.plaoc.microService.sys.plugin.systemui


import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.toJsonAble
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class StatusBarNMM : NativeMicroModule("statusbar.sys.dweb") { // 小写不然路由不到

    private fun getController(mmid: Mmid) =
        NativeUiController.fromMultiWebView(mmid).statusBar

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {

        apiRouting = routes(
            /** 设置状态栏背景色*/
            "/setBackgroundColor" bind Method.GET to defineHandler { request, ipc ->
                val color = QueryHelper.color(request)
                getController(ipc.remote.mmid).colorState.value = color
                return@defineHandler null
            },
            /** 获取状态栏背景色*/
            "/getBackgroundColor" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler getController(ipc.remote.mmid).colorState.value
            },
            /** 设置状态栏风格*/
            "/setForegroundStyle" bind Method.GET to defineHandler { request, ipc ->
                val style = QueryHelper.style(request)
                getController(ipc.remote.mmid).styleState.value = style
                return@defineHandler null
            },
            /** 获取状态栏风格*/
            "/getForegroundStyle" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler getController(ipc.remote.mmid).styleState.value
            },
            /** 获取状态栏信息*/
            "/getInfo" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler getController(ipc.remote.mmid)
            },
            /** 设置状态栏是否覆盖webview*/
            "/setOverlays" bind Method.GET to defineHandler { request, ipc ->
                val overlay = QueryHelper.overlay(request)
                getController(ipc.remote.mmid).overlayState.value = overlay
                return@defineHandler null
            },
            /** 设置状态栏是否覆盖webview (透明)*/
            "/getOverlays" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler getController(ipc.remote.mmid).overlayState.value
            },
            /** 设置状态栏是否可见 */
            "/setVisible" bind Method.GET to defineHandler { request, ipc ->
                val visible = QueryHelper.visible(request)
                getController(ipc.remote.mmid).visibleState.value = visible
                return@defineHandler null
            },
            /** 获取状态栏是否可见 */
            "/getVisible" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler getController(ipc.remote.mmid).visibleState.value
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}
