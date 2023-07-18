package info.bagen.dwebbrowser.microService.desktop

import android.content.Intent
import android.os.Bundle
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.BrowserNMM
import info.bagen.dwebbrowser.microService.browser.debugBrowser
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import info.bagen.dwebbrowser.microService.browser.jmm.JmmNMM
import info.bagen.dwebbrowser.microService.browser.jmm.debugJMM
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class DesktopNMM : NativeMicroModule("desktop.browser.dweb") {

  companion object {
    private val controllerList = mutableListOf<DesktopController>()
    val desktopController get() = controllerList.firstOrNull()
  }

  init {
    controllerList.add(DesktopController(this))
  }

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

  override suspend fun onActivity(event: IpcEvent, ipc: Ipc) {
    App.startActivity(DesktopActivity::class.java) { intent ->
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
      // 由于SplashActivity添加了android:excludeFromRecents属性，导致同一个task的其他activity也无法显示在Recent Screen，比如BrowserActivity
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
      intent.putExtras(Bundle().also { b -> b.putString("mmid", mmid) })
    }
  }
}