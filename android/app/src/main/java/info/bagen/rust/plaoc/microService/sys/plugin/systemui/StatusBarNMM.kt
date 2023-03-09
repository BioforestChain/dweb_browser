package info.bagen.rust.plaoc.microService.sys.plugin.systemui


import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM.Companion.getCurrentWebViewController
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class StatusBarNMM:NativeMicroModule("statusbar.sys.dweb") { // 小写不然路由不到

    private val statusBar  by lazy {
        val currentView =  getCurrentWebViewController()?.activity?.systemUiPlugin
        currentView
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext)
 {
        apiRouting = routes(
            /** 设置状态栏背景色*/
            "/setBackgroundColor" bind Method.GET to defineHandler { request ->
                val colorHex = Query.string().required("color")(request)
                val result = statusBar?.setStatusBarBackgroundColor(colorHex).toString()
//                println("StatusBarNMM#apiRouting setBackgroundColor===>$colorHex  $result ")
                Response(Status.OK).body(result)
            },
            /** 获取状态栏背景色*/
            "/getBackgroundColor" bind Method.GET to defineHandler { request ->
                //  println("StatusBarNMM#apiRouting getBackgroundColor===>$result")
                return@defineHandler statusBar?.getStatusBarBackgroundColor()
            },
            /** 设置状态栏风格*/
            "/setStyle" bind Method.GET to defineHandler { request ->
                val style = Query.string().required("style")(request)
//                println("StatusBarNMM#apiRouting setStyle===>$style  ")
                return@defineHandler statusBar?.setStatusBarStyle(style)
            },
            /** 获取状态栏信息*/
            "/getInfo" bind Method.GET to defineHandler { request ->
                val visible = statusBar?.getStatusBarVisible()
                val style = statusBar?.getStatusBarIsDark()
                val overlay = statusBar?.getStatusBarOverlay()
                val color = statusBar?.getStatusBarColor()
                //                println("StatusBarNMM#apiRouting getInfo===>$result")
                return@defineHandler """{"visible":$visible,"style":$style,"overlay":$overlay,"color":$color}"""
            },
            /** 设置状态栏是否覆盖webview (透明)*/
            "/setOverlays" bind Method.GET to defineHandler { request ->
                val overlay = Query.boolean().required("overlay")(request)
                val result = statusBar?.setStatusBarOverlay(overlay)
                println("StatusBarNMM#apiRouting setOverlays===>$overlay  $result ")

               return@defineHandler result
            },
            /** 设置状态栏是否可见 */
            "/setVisible" bind Method.GET to defineHandler { request ->
                val visible = Query.boolean().required("visible")(request)
                println("StatusBarNMM#apiRouting setVisible===>$visible   ")
                return@defineHandler statusBar?.setStatusBarVisible(visible)
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}