package info.bagen.dwebbrowser.microService.desktop

import info.bagen.dwebbrowser.microService.browser.BrowserNMM
import info.bagen.dwebbrowser.microService.browser.debugBrowser
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import info.bagen.dwebbrowser.microService.browser.jmm.JmmNMM
import info.bagen.dwebbrowser.microService.browser.jmm.debugJMM
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class DesktopNMM : NativeMicroModule("desktop.browser.dweb") {
    val queryAppId = Query.string().required("app_id")
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
        "/openAppOrActivate" bind Method.GET to defineHandler { request ->
            val mmid = queryAppId(request)
            val (ipc) = bootstrapContext.dns.connect(mmid)
            debugJMM("openApp", "postMessage==>activity ${ipc.remote.mmid}")
            ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
            return@defineHandler true
        },
        "/appsInfo" bind Method.GET to defineHandler { request ->
            val apps = JmmNMM.getAndUpdateJmmNmmApps()
            debugBrowser("appInfo", apps.size)
            val responseApps = mutableListOf<BrowserNMM.AppInfo>()
            apps.forEach { item ->
                val meta = item.value.metadata
                responseApps.add(
                    BrowserNMM.AppInfo(
                        meta.id,
                        meta.icon,
                        meta.name,
                        meta.short_name
                    )
                )
            }
            return@defineHandler responseApps
        },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}