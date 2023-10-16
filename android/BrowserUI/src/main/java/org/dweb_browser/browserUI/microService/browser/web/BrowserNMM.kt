package org.dweb_browser.browserUI.microService.browser.web

import kotlinx.coroutines.withContext
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.microservice.core.AndroidNativeMicroModule
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.DwebHttpServerOptions
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.microservice.sys.http.createHttpDwebServer
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugBrowser(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("browser", tag, msg, err)

class BrowserNMM : AndroidNativeMicroModule("web.browser.dweb", "Web Browser") {
  init {
    short_name = "Browser";
    dweb_deeplinks = listOf("dweb:search", "dweb:openinbrowser")
    categories = listOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))
  }

  private lateinit var browserServer: HttpDwebServer
  val queryAppId = Query.string().optional("mmid")
  val queryKeyWord = Query.string().required("q")
  val queryUrl = Query.string().required("url")

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    browserServer = this.createBrowserWebServer()
    val browserController = // 由于 WebView创建需要在主线程，所以这边做了 withContext 操作
      withMainContext {
        BrowserController(
          this@BrowserNMM, browserServer
        )
      }

    onActivity {
      browserController.openBrowserWindow()
    }

    apiRouting = routes(
      "search" bind Method.GET to defineHandler { request ->
        debugBrowser("do search", request.uri)
        val search = queryKeyWord(request)
        browserController.openBrowserWindow(search = search)
        return@defineHandler Response(Status.OK)
      },
      "openinbrowser" bind Method.GET to defineHandler { request ->
        debugBrowser("do openinbrowser", request.uri)
        val url = queryUrl(request)
        browserController.openBrowserWindow(url = url)
        return@defineHandler Response(Status.OK)
      },
      "/uninstall" bind Method.GET to defineHandler { request, ipc ->
        debugBrowser("uninstall", request.uri)
        val mmid = queryAppId(request)
        if (mmid == null) {
          browserController.uninstallWindow()
        }
        //TODO 卸载webApp
        return@defineHandler Response(Status.OK)
      },
    )
  }

  private val API_PREFIX = "/api/"
  private suspend fun createBrowserWebServer(): HttpDwebServer {
    val browserServer = createHttpDwebServer(DwebHttpServerOptions(subdomain = "", port = 433))
    browserServer.listen().onRequest { (request, ipc) ->
      val pathName = request.uri.path
      debugBrowser("createBrowserWebServer", pathName)
      if (!pathName.startsWith(API_PREFIX)) {
        val response = nativeFetch("file:///sys/browser/web${pathName}?mode=stream")
        ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
      }
    }
    return browserServer
  }

  override suspend fun _shutdown() {
  }
}