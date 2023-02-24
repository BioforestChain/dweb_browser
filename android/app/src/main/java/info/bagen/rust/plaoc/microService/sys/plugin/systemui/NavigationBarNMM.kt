package info.bagen.rust.plaoc.microService.sys.plugin.systemui

import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.sys.plugin.clipboard.ClipboardNMM
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class NavigationBarNMM : NativeMicroModule("navigationBar.sys.dweb") {

    private val nav = run {
        App.mainActivity?.dWebBrowserModel?.getSystemUi()
    }

    override suspend fun _bootstrap() {
        apiRouting = routes(
            /** 设置系统导航栏颜色*/
            "/setBackgroundColor" bind Method.GET to defineHandler { request ->
                println("NavigationBarNMM#apiRouting setNavigationBarColor===>$mmid  ${request.uri.path} ")
                val colorHex = Query.string().required("color")(request)
                val result = nav?.setStatusBarBackgroundColor(colorHex).toString()
                Response(Status.OK).body(result)
            },
            /** 获取当前导航栏背景色*/
            "/getBackgroundColor" bind Method.GET to defineHandler { request ->
                println("NavigationBarNMM#apiRouting getNavigationBarColor===>$mmid  ${request.uri.path} ")
                val result = nav?.getNavigationBarColor().toString()
                Response(Status.OK).body(result)
            },
            /** 获取当前导航栏是否可见*/
            "/getVisible" bind Method.GET to defineHandler { request ->
                println("NavigationBarNMM#apiRouting setNavigationBarColor===>$mmid  ${request.uri.path} ")
                val result = nav?.getNavigationBarVisible().toString()
                Response(Status.OK).body(result)
            },
            /** 设置当前导航栏是否可见*/
            "/setVisible" bind Method.GET to defineHandler { request ->
                println("NavigationBarNMM#apiRouting setNavigationBarColor===>$mmid  ${request.uri.path} ")
                val visible = Query.boolean().required("visible")(request)
                val result = nav?.setNavigationBarVisible(visible).toString()
                Response(Status.OK).body(result)
            },
            /** 获取系统导航栏是否透明*/
            "/getOverlay" bind Method.GET to defineHandler { request ->
                println("NavigationBarNMM#apiRouting setNavigationBarColor===>$mmid  ${request.uri.path} ")
                val result = nav?.getNavigationBarOverlay().toString()
                Response(Status.OK).body(result)
            },
            /** 设置系统导航栏是否透明*/
            "/setOverlay" bind Method.GET to defineHandler { request ->
                println("NavigationBarNMM#apiRouting setNavigationBarColor===>$mmid  ${request.uri.path} ")
                val isOverlay = Query.boolean().required("isOverlay")(request)
                val result = nav?.setNavigationBarOverlay(isOverlay).toString()
                Response(Status.OK).body(result)
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}