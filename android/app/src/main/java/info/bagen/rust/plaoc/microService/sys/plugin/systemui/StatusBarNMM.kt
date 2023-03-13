package info.bagen.rust.plaoc.microService.sys.plugin.systemui


import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM.Companion.getCurrentWebViewController
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class StatusBarNMM : NativeMicroModule("statusbar.sys.dweb") { // 小写不然路由不到

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            /** 设置状态栏背景色*/
            "/setBackgroundColor" bind Method.GET to defineHandler { request, ipc ->
                val colorHex = Query.string().required("color")(request)
                val statusBar = getStatusBar(ipc.remote.mmid)
                val result = statusBar.setStatusBarBackgroundColor(colorHex).toString()
//                println("StatusBarNMM#apiRouting setBackgroundColor===>$colorHex  $result ${ipc.remote.mmid}")
                Response(Status.OK).body(result)
            },
            /** 获取状态栏背景色*/
            "/getBackgroundColor" bind Method.GET to defineHandler { request, ipc ->
                //  println("StatusBarNMM#apiRouting getBackgroundColor===>$result")
                val statusBar = getStatusBar(ipc.remote.mmid)
                return@defineHandler statusBar.getStatusBarBackgroundColor()
            },
            /** 设置状态栏风格*/
            "/setStyle" bind Method.GET to defineHandler { request, ipc ->
                val style = Query.string().required("style")(request)
//                println("StatusBarNMM#apiRouting setStyle===>$style  ")
                val statusBar = getStatusBar(ipc.remote.mmid)
                return@defineHandler statusBar.setStatusBarStyle(style)
            },
            /** 获取状态栏信息*/
            "/getInfo" bind Method.GET to defineHandler { request, ipc ->
                val statusBar = getStatusBar(ipc.remote.mmid)
                val visible = statusBar.getStatusBarVisible()
                val style = statusBar.getStatusBarIsDark()
                val overlay = statusBar.getStatusBarOverlay()
                val color = statusBar.getStatusBarColor()
                // println("StatusBarNMM#apiRouting getInfo===>$result")
                return@defineHandler StatusBarInfo(visible, style, overlay, color)
            },
            /** 设置状态栏是否覆盖webview*/
            "/setOverlays" bind Method.GET to defineHandler { request,ipc ->
                val overlay = Query.boolean().required("overlay")(request)
                val statusBar = getStatusBar(ipc.remote.mmid)
                val result = statusBar.setStatusBarOverlay(overlay)
//                println("StatusBarNMM#apiRouting setOverlays===>$overlay  $result ")
                return@defineHandler result
            },
            /** 设置状态栏是否覆盖webview (透明)*/
            "/getOverlays" bind Method.GET to defineHandler { request, ipc ->
                val statusBar = getStatusBar(ipc.remote.mmid)
                val result = statusBar.getStatusBarOverlay()
//                println("StatusBarNMM#apiRouting setOverlays===>$overlay  $result ")
                return@defineHandler result
            },
            /** 设置状态栏是否可见 */
            "/setVisible" bind Method.GET to defineHandler { request, ipc ->
                val visible = Query.boolean().required("visible")(request)
                val statusBar = getStatusBar(ipc.remote.mmid)
//                println("StatusBarNMM#apiRouting setVisible===>$visible   ")
                return@defineHandler statusBar.setStatusBarVisible(visible)
            },
        )
    }

    private fun getStatusBar(mmid: Mmid): SystemUiPlugin {
        return getCurrentWebViewController(mmid)?.webViewList?.last()?.systemUiPlugin
            ?: throw Exception("system ui is unavailable for $mmid")
    }

    data class StatusBarInfo(
        val visible: Boolean?,
        val style: String?,
        val overlay: Boolean?,
        val color: String?
    )

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}
