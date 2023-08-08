package info.bagen.dwebbrowser.microService.browser.web

import android.content.Intent
import android.os.Bundle
import android.util.Log
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.DwebHttpServerOptions
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.microservice.sys.http.createHttpDwebServer
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.UUID

fun debugBrowser(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("browser", tag, msg, err)

class BrowserNMM : AndroidNativeMicroModule("web.browser.dweb", "Web Browser") {
  override val short_name = "Browser";
  override val dweb_deeplinks = listOf("dweb:search", "dweb:openinbrowser")
  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
  override val icons: List<ImageResource> =
    listOf(ImageResource(src = "file:///sys/browser/web/logo.svg"))

  companion object {
    val controllers = mutableMapOf<String, BrowserController>()
  }

  val queryAppId = Query.string().required("app_id")
  val queryKeyWord = Query.string().required("q")
  val queryUrl = Query.string().required("url")
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val browserServer = this.createBrowserWebServer()
    val controller = BrowserController(this, browserServer)
    val sessionId = UUID.randomUUID().toString()
    controllers[sessionId] = controller

    this.onAfterShutdown {
      controllers.remove(sessionId)
    }

    onActivity {
      openView(sessionId)
    }

    apiRouting = routes(
      "search" bind Method.GET to defineHandler { request ->
        debugBrowser("do search", request.uri)
        val search = queryKeyWord(request)
        openView(sessionId = sessionId, search = search)
        return@defineHandler true
      },
      "openinbrowser" bind Method.GET to defineHandler { request ->
        debugBrowser("do openinbrowser", request.uri)
        val url = queryUrl(request)
        openView(sessionId = sessionId, url = url)
        return@defineHandler true
      }
    )
  }

  private fun openView(sessionId: String, search: String? = null, url: String? = null) {
    App.startActivity(BrowserActivity::class.java) { intent ->
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
      // 由于SplashActivity添加了android:excludeFromRecents属性，导致同一个task的其他activity也无法显示在Recent Screen，比如BrowserActivity
      // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
      /*intent.putExtras(Bundle().apply {
        putString("sessionId", sessionId)
        putString("mmid", mmid)
      })*/
      intent.putExtra("sessionId", sessionId)
      intent.putExtra("mmid", mmid)
      search?.let { intent.putExtra("search", search) }
      url?.let { intent.putExtra("url", url) }
    }
  }

  private val API_PREFIX = "/api/"
  private suspend fun createBrowserWebServer(): HttpDwebServer {
    val browserServer =
      createHttpDwebServer(DwebHttpServerOptions(subdomain = "", port = 433))
    browserServer.listen().onRequest { (request, ipc) ->
      val pathName = request.uri.path
      debugBrowser("createBrowserWebServer", pathName)
      if (!pathName.startsWith(API_PREFIX)) {
        val response =
          nativeFetch("file:///sys/browser/desk${pathName}?mode=stream")
        ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
      }
    }
    return browserServer
  }

  override suspend fun _shutdown() {
  }
}