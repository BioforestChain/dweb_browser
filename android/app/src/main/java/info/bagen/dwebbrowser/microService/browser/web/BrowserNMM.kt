package info.bagen.dwebbrowser.microService.browser.web

import android.content.Intent
import android.os.Bundle
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import info.bagen.dwebbrowser.microService.browser.jmm.debugJMM
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugBrowser(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("browser", tag, msg, err)

class BrowserNMM : AndroidNativeMicroModule("web.browser.dweb", "Web Browser") {
  override val short_name = "Browser";
  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
  override val icons: List<ImageResource> = listOf(ImageResource(src = "file:///sys/browser/web/logo.svg"))

  companion object {
    val controllerList = mutableListOf<BrowserController>()
    val browserController get() = controllerList.firstOrNull() // 只能browser 里面调用，不能给外部调用
  }

  init {
    controllerList.add(BrowserController(this))
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
    )
  }

  override suspend fun onActivity(event: IpcEvent, ipc: Ipc) {
    App.startActivity(BrowserActivity::class.java) { intent ->
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
      // 由于SplashActivity添加了android:excludeFromRecents属性，导致同一个task的其他activity也无法显示在Recent Screen，比如BrowserActivity
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
      intent.putExtras(Bundle().also { b -> b.putString("mmid", mmid) })
    }
  }

  override suspend fun _shutdown() {
  }
}