package info.bagen.dwebbrowser.microService.browser.web

import android.content.Intent
import android.os.Bundle
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.desk.DesktopNMM
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import info.bagen.dwebbrowser.microService.browser.jmm.debugJMM
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.DwebHttpServerOptions
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.microservice.sys.http.createHttpDwebServer
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.UUID

fun debugBrowser(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("browser", tag, msg, err)

class BrowserNMM : AndroidNativeMicroModule("web.browser.dweb", "Web Browser") {
  override val short_name = "Browser";
  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
  override val icons: List<ImageResource> =
    listOf(ImageResource(src = "file:///sys/browser/web/logo.svg"))

  companion object {
    val controllers = mutableMapOf<String,BrowserController>()
  }

  val queryAppId = Query.string().required("app_id")
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val browserServer = this.createDesktopWebServer()
    val controller = BrowserController(this, browserServer)
    val sessionId = UUID.randomUUID().toString()
    controllers[sessionId] = controller

    this.onAfterShutdown {
      DesktopNMM.controllers.remove(sessionId)
    }

    apiRouting = routes(
      "/openAppOrActivate" bind Method.GET to defineHandler { request ->
        val mmid = queryAppId(request)
        val (ipc) = bootstrapContext.dns.connect(mmid)
        debugJMM("openApp", "postMessage==>activity ${ipc.remote.mmid}")
        ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
        return@defineHandler true
      },
    )

    onActivity {
      App.startActivity(BrowserActivity::class.java) { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        // 由于SplashActivity添加了android:excludeFromRecents属性，导致同一个task的其他activity也无法显示在Recent Screen，比如BrowserActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        intent.putExtras(Bundle().apply {
          putString("sessionId", sessionId)
          putString("mmid", mmid)
        })
      }
    }
  }

  private val API_PREFIX = "/api/"
  private suspend fun createDesktopWebServer(): HttpDwebServer {
    val desktopServer =
      createHttpDwebServer(DwebHttpServerOptions(subdomain = "desktop", port = 433))
    desktopServer.listen().onRequest { (request, ipc) ->
      val pathName = request.uri.path
      val url = if (pathName.startsWith(API_PREFIX)) {
        val internalUri = request.uri.path(request.uri.path.substring(API_PREFIX.length))
        val search = ""
        "file://$internalUri$search"
      } else {
        "file:///sys/browser/desk${pathName}?mode=stream"
      }
      val response =
        nativeFetch(url)
      ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
    }
    return desktopServer
  }

  override suspend fun _shutdown() {
  }
}