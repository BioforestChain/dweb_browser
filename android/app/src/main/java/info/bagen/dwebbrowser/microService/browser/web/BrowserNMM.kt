package info.bagen.dwebbrowser.microService.browser.web

import info.bagen.dwebbrowser.microService.browser.desk.DeskLinkMetaData
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.createWindowAdapterManager
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.ipc.Ipc
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

  private var browserController: BrowserController? = null
  private lateinit var browserServer: HttpDwebServer

  val queryAppId = Query.string().required("app_id")
  val queryKeyWord = Query.string().required("q")
  val queryUrl = Query.string().required("url")
  private val runningWebApps = mutableListOf<DeskLinkMetaData>()

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    browserServer = this.createBrowserWebServer()
//    val sessionId = UUID.randomUUID().toString()

    this.onAfterShutdown {
      // controllers.remove(sessionId)
      browserController = null
    }

    onActivity {
      // openView(sessionId)
      openBrowserWindow(it.second)
    }

    apiRouting = routes(
      "search" bind Method.GET to defineHandler { request, ipc ->
        debugBrowser("do search", request.uri)
        val search = queryKeyWord(request)
        // openView(sessionId = sessionId, search = search)
        openBrowserWindow(ipc, search = search)
        return@defineHandler true
      },
      "openinbrowser" bind Method.GET to defineHandler { request, ipc ->
        debugBrowser("do openinbrowser", request.uri)
        val url = queryUrl(request)
        // openView(sessionId = sessionId, url = url)
        openBrowserWindow(ipc, url = url)
        return@defineHandler true
      },
//      "/browser/observe/apps" bind Method.GET to defineHandler { _, ipc ->
//        val inputStream = ReadableStream(onStart = { controller ->
//          val off = browserController.onUpdate {
//            try {
//              withContext(Dispatchers.IO) {
//                controller.enqueue((gson.toJson(runningWebApps) + "\n").toByteArray())
//              }
//            } catch (e: Exception) {
//              controller.close()
//              e.printStackTrace()
//            }
//          }
//          ipc.onClose {
//            off()
//            controller.close()
//          }
//        })
//        browserController?.updateSignal?.emit()
//        return@defineHandler Response(Status.OK).body(inputStream)
//      },
      "/uninstall" bind Method.GET to defineHandler { request, ipc ->
        debugBrowser("uninstall", request.uri)
        val search = queryKeyWord(request)
        // openView(sessionId = sessionId, search = search)
        openBrowserWindow(ipc, search = search)
        return@defineHandler true
      },
    )
  }

//  private fun openView(sessionId: String, search: String? = null, url: String? = null) {
//    App.startActivity(BrowserActivity::class.java) { intent ->
//      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
//      intent.putExtra("sessionId", sessionId)
//      intent.putExtra("mmid", mmid)
//      search?.let { intent.putExtra("search", search) }
//      url?.let { intent.putExtra("url", url) }
//    }
//  }

  private suspend fun openBrowserWindow(ipc: Ipc, search: String? = null, url: String? = null) {
    // 打开安装窗口
    val win = createWindowAdapterManager.createWindow(
      WindowState(owner = ipc.remote.mmid, provider = mmid, microModule = this).also {
        it.mode = WindowMode.MAXIMIZE
      }
    )
    // 由于 WebView创建需要在主线程，所以这边做了 withContext 操作
    withContext(mainAsyncExceptionHandler) {
      browserController =
        BrowserController(win, this@BrowserNMM, browserServer).also { controller ->

          search?.let { controller.updateDWSearch(it) }
          url?.let { controller.updateDWUrl(it) }
        }
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