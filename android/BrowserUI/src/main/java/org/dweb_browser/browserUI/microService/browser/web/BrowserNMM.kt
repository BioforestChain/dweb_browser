package org.dweb_browser.browserUI.microService.browser.web

import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browserUI.microService.browser.types.DeskLinkMetaData
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.AndroidNativeMicroModule
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.ipc.helper.ReadableStream
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
    dweb_deeplinks += listOf("dweb:search", "dweb:openinbrowser")
    categories = mutableListOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
    icons += listOf(ImageResource(src = "file:///sys/browser/web/logo.svg"))
  }

  private lateinit var browserServer: HttpDwebServer
  val queryAppId = Query.string().optional("mmid")
  val queryKeyWord = Query.string().required("q")
  val queryUrl = Query.string().required("url")

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    browserServer = this.createBrowserWebServer()
    val browserController = // 由于 WebView创建需要在主线程，所以这边做了 withContext 操作
      withContext(mainAsyncExceptionHandler) {
        BrowserController(
          this@BrowserNMM, browserServer
        )
      }

    onActivity {
      browserController.openBrowserWindow(it.second).also { win ->
        win.focus()
      }
    }

    apiRouting = routes(
      "search" bind Method.GET to defineHandler { request, ipc ->
        debugBrowser("do search", request.uri)
        val search = queryKeyWord(request)
        browserController.openBrowserWindow(ipc, search = search)
        return@defineHandler Response(Status.OK)
      },
      "openinbrowser" bind Method.GET to defineHandler { request, ipc ->
        debugBrowser("do openinbrowser", request.uri)
        val url = queryUrl(request)
        // openView(sessionId = sessionId, url = url)
        browserController.openBrowserWindow(ipc, url = url)
        return@defineHandler Response(Status.OK)
      },
      "/browser/observe/apps" bind Method.GET to defineHandler { _, ipc ->
        debugBrowser("/browser/observe/apps", ipc.remote.mmid)
        val inputStream = ReadableStream(onStart = { controller ->
          val off = browserController.onUpdate {
            debugBrowser("/browser/observe/apps", "onUpdate -> ${browserController.runningWebApps.size}")
            try {
              controller.enqueue((Json.encodeToString(browserController.runningWebApps) + "\n").toByteArray())
            } catch (e: Exception) {
              controller.close()
              e.printStackTrace()
            }
          }
          ipc.onClose {
            off()
            controller.close()
          }
        })
        browserController.updateSignal.emit()
        return@defineHandler Response(Status.OK).body(inputStream)
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
      "/addToDesktop" bind Method.GET to defineHandler { request, ipc ->
        debugBrowser("addToDesktop", request.uri)
        val mmid = queryAppId(request)
        if (mmid == null) {
          browserController.uninstallWindow()
        }
        //TODO 卸载webApp
        return@defineHandler true
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
        val response = nativeFetch("file:///sys/browser/desk${pathName}?mode=stream")
        ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
      }
    }
    return browserServer
  }

  override suspend fun _shutdown() {
  }
}