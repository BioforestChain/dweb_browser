package info.bagen.rust.plaoc.microService.sys.plugin.systemui

import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class StatusBarNMM:NativeMicroModule("statusBar.sys.dweb") {

    private val statusBar = run {
        App.browserActivity?.dWebBrowserModel?.getSystemUi()
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext)
 {
        apiRouting = routes(
            /** 设置状态栏背景色*/
            "/setBackgroundColor" bind Method.GET to defineHandler { request ->
                println("StatusBarNMM#apiRouting setBackgroundColor===>$mmid  ${request.uri.path} ")
                val colorHex = Query.string().required("color")(request)
                val result = statusBar?.setStatusBarBackgroundColor(colorHex).toString()
                Response(Status.OK).body(result)
            },
            /** 设置状态栏风格*/
            "/setStyle" bind Method.GET to defineHandler { request ->
                println("StatusBarNMM#apiRouting setStyle===>$mmid  ${request.uri.path} ")
                val style = Query.boolean().required("style")(request)
                val result = statusBar?.setStatusBarStyle(style).toString()
                Response(Status.OK).body(result)
            },
            /** 获取状态栏风格*/
            "/getStyle" bind Method.GET to defineHandler { request ->
                println("StatusBarNMM#apiRouting getStyle===>$mmid  ${request.uri.path} ")
                val result = statusBar?.getStatusBarIsDark().toString()
                Response(Status.OK).body(result)
            },
            /** 设置状态栏是否覆盖webview (透明)*/
            "/setOverlays" bind Method.GET to defineHandler { request ->
                println("StatusBarNMM#apiRouting setOverlays===>$mmid  ${request.uri.path} ")
                val overlay = Query.boolean().required("overlay")(request)
                val result = statusBar?.setStatusBarOverlay(overlay).toString()
                Response(Status.OK).body(result)
            },
            /** 获取状态栏是否覆盖webview (透明)*/
            "/getOverlays" bind Method.GET to defineHandler { request ->
                println("StatusBarNMM#apiRouting getOverlays===>$mmid  ${request.uri.path} ")
                val result = statusBar?.getStatusBarOverlay().toString()
                Response(Status.OK).body(result)
            },
            /** 设置状态栏是否可见 */
            "/setVisible" bind Method.GET to defineHandler { request ->
                println("StatusBarNMM#apiRouting setVisible===>$mmid  ${request.uri.path} ")
                val visible = Query.boolean().required("visible")(request)
                val result = statusBar?.setStatusBarVisible(visible).toString()
                Response(Status.OK).body(result)
            },
            /** 设置状态栏是否可见 */
            "/getVisible" bind Method.GET to defineHandler { request ->
                println("StatusBarNMM#apiRouting getVisible===>$mmid  ${request.uri.path} ")
                val result = statusBar?.getStatusBarVisible().toString()
                Response(Status.OK).body(result)
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}