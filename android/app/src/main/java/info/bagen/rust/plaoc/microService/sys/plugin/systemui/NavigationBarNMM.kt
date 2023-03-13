package info.bagen.rust.plaoc.microService.sys.plugin.systemui

import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class NavigationBarNMM : NativeMicroModule("navigationbar.sys.dweb") {

    private fun getNav(mmid: Mmid): SystemUiPlugin? {
        return MultiWebViewNMM.getCurrentWebViewController(mmid)?.webViewList?.last()?.systemUiPlugin
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            /** 设置系统导航栏颜色*/
            "/setBackgroundColor" bind Method.GET to defineHandler { request, ipc ->
                val colorHex = Query.string().required("color")(request)
                val darkIcons = Query.boolean().defaulted("darkIcons", true)(request)
                val contrast = Query.boolean().defaulted("contrast", true)(request)
//                println("NavigationBarNMM#apiRouting setNavigationBarColor===>$colorHex")
                getNav(ipc.remote.mmid)?.setNavigationBarColor(colorHex, darkIcons, contrast)
                Response(Status.OK)
            },
            /** 获取当前导航栏背景色*/
            "/getBackgroundColor" bind Method.GET to defineHandler { request, ipc ->
                val result = getNav(ipc.remote.mmid)?.getNavigationBarColor().toString()
                Response(Status.OK).body(result)
            },
            /** 获取当前导航栏是否可见*/
            "/getVisible" bind Method.GET to defineHandler { request, ipc ->
                val result = getNav(ipc.remote.mmid)?.getNavigationBarVisible()
                println("NavigationBarNMM#apiRouting getVisible===>$result")
                return@defineHandler result
            },
            /** 设置当前导航栏是否可见*/
            "/setVisible" bind Method.GET to defineHandler { request, ipc ->
                val visible = Query.boolean().required("visible")(request)
                val result = getNav(ipc.remote.mmid)?.setNavigationBarVisible(visible)
                println("NavigationBarNMM#apiRouting setVisible===>visible: $visible  $result")
                return@defineHandler result
            },
            /** 获取系统导航栏是否透明*/
            "/getTransparency" bind Method.GET to defineHandler { request, ipc ->
                val result = getNav(ipc.remote.mmid)?.getNavigationBarTransparency()
                println("NavigationBarNMM#apiRouting getTransparency===>$result")
                return@defineHandler result
            },
            /** 设置系统导航栏是否透明*/
            "/setTransparency" bind Method.GET to defineHandler { request, ipc ->
                val isOverlay = Query.boolean().required("isTransparency")(request)
                println("NavigationBarNMM#apiRouting setTransparency===>  $isOverlay ")
                val result = getNav(ipc.remote.mmid)?.setNavigationBarTransparency(isOverlay)
                return@defineHandler result
            },
            /** 获取系统导航栏是否覆盖内容*/
            "/getOverlay" bind Method.GET to defineHandler { request, ipc ->
                val result = getNav(ipc.remote.mmid)?.getNavigationBarOverlay()
                println("NavigationBarNMM#apiRouting getOverlay===>$result")
                return@defineHandler result
            },
            /** 设置系统导航栏是否覆盖内容*/
            "/setOverlay" bind Method.GET to defineHandler { request, ipc ->
                val isOverlay = Query.boolean().required("isOverlay")(request)
                println("NavigationBarNMM#apiRouting setOverlay===>  $isOverlay ")
                val result = getNav(ipc.remote.mmid)?.setNavigationBarOverlay(isOverlay)
                return@defineHandler result
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}