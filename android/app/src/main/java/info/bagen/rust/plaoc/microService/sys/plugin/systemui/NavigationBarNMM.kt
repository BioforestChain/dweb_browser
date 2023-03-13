package info.bagen.rust.plaoc.microService.sys.plugin.systemui

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.toJsonAble
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class NavigationBarNMM : NativeMicroModule("navigationbar.sys.dweb") {


    private fun getController(mmid: Mmid) =
        NativeUiController.fromMultiWebView(mmid).navigationBar

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            /** 设置系统导航栏颜色*/
            "/setBackgroundColor" bind Method.GET to defineHandler { request, ipc ->
                val color = QueryHelper.color(request)
                getController(ipc.remote.mmid).colorState.value = color
                return@defineHandler null
            },
            /** 获取当前导航栏背景色*/
            "/getBackgroundColor" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler getController(ipc.remote.mmid).colorState.value.toJsonAble()
            },
            /** 设置系统导航栏颜色*/
            "/setForegroundStyle" bind Method.GET to defineHandler { request, ipc ->
                val style = QueryHelper.style(request)
                getController(ipc.remote.mmid).styleState.value = style
                return@defineHandler null
            },
            /** 获取当前导航栏背景色*/
            "/getForegroundStyle" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler getController(ipc.remote.mmid).styleState.value
            },
            /** 获取当前导航栏是否可见*/
            "/getVisible" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler getController(ipc.remote.mmid).visibleState.value
            },
            /** 设置当前导航栏是否可见*/
            "/setVisible" bind Method.GET to defineHandler { request, ipc ->
                val visible = QueryHelper.visible(request)
                getController(ipc.remote.mmid).visibleState.value = visible
                return@defineHandler null
            },
//            /** 获取系统导航栏是否透明*/
//            "/getTransparency" bind Method.GET to defineHandler { request, ipc ->
//                val result = getController(ipc.remote.mmid)?.getNavigationBarTransparency()
//                println("NavigationBarNMM#apiRouting getTransparency===>$result")
//                return@defineHandler result
//            },
//            /** 设置系统导航栏是否透明*/
//            "/setTransparency" bind Method.GET to defineHandler { request, ipc ->
//                val isOverlay = Query.boolean().required("isTransparency")(request)
//                println("NavigationBarNMM#apiRouting setTransparency===>  $isOverlay ")
//                val result = getController(ipc.remote.mmid)?.setNavigationBarTransparency(isOverlay)
//                return@defineHandler result
//            },
            /** 获取系统导航栏是否覆盖内容*/
            "/getOverlay" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler getController(ipc.remote.mmid).overlayState.value
            },
            /** 设置系统导航栏是否覆盖内容*/
            "/setOverlay" bind Method.GET to defineHandler { request, ipc ->
                val overlay = QueryHelper.overlay(request)
                getController(ipc.remote.mmid).overlayState.value = overlay
                return@defineHandler null
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}