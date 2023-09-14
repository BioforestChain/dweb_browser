package org.dweb_browser.browserUI.microService.browser.web

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.AndroidNativeMicroModule
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.http.bindDwebDeeplink
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.DwebHttpServerOptions
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.microservice.sys.http.createHttpDwebServer

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

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    browserServer = this.createBrowserWebServer()
    val browserController = // 由于 WebView创建需要在主线程，所以这边做了 withContext 操作
      withContext(mainAsyncExceptionHandler) {
        BrowserController(
          this@BrowserNMM, browserServer
        )
      }

    onActivity {
      browserController.openBrowserWindow()
    }

    routes(
      "search" bindDwebDeeplink definePureResponse {
        debugBrowser("do search", request.url)
        browserController.openBrowserWindow(search = request.queryOrFail("q"))
        return@definePureResponse PureResponse(HttpStatusCode.OK)
      },
      "openinbrowser" bindDwebDeeplink definePureResponse {
        debugBrowser("do openinbrowser", request.url)
        browserController.openBrowserWindow(url = request.queryOrFail("url"))
        return@definePureResponse PureResponse(HttpStatusCode.OK)
      },
      "/uninstall" bind HttpMethod.Get to definePureResponse {
        debugBrowser("uninstall", request.url)
        val mmid = request.query("mmid")
        if (mmid == null) {
          browserController.uninstallWindow()
        }
        //TODO 卸载webApp
        return@definePureResponse PureResponse(HttpStatusCode.OK)
      },
    )
  }

  private val API_PREFIX = "/api/"
  private suspend fun createBrowserWebServer(): HttpDwebServer {
    val browserServer = createHttpDwebServer(DwebHttpServerOptions(subdomain = "", port = 433))
    browserServer.listen().onRequest { (request, ipc) ->
      val pathName = request.uri.encodedPath
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