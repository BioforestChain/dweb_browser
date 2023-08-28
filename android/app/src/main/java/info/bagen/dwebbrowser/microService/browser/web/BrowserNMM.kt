package info.bagen.dwebbrowser.microService.browser.web

import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.browser.desk.DeskLinkMetaData
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.DwebHttpServerOptions
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.microservice.sys.http.createHttpDwebServer
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.constant.WindowConstants
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.core.createWindowAdapterManager
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
  override val short_name = "Browser";
  override val dweb_deeplinks = listOf("dweb:search", "dweb:openinbrowser")
  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
  override val icons: List<ImageResource> =
    listOf(ImageResource(src = "file:///sys/browser/web/logo.svg"))

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
      openBrowserWindow(it.second).also { win ->
        win.focus()
      }
    }

    apiRouting = routes(
      "search" bind Method.GET to defineHandler { request, ipc ->
        debugBrowser("do search", request.uri)
        val search = queryKeyWord(request)
        // openView(sessionId = sessionId, search = search)
        openBrowserWindow(ipc, search = search)
        return@defineHandler Response(Status.OK)
      },
      "openinbrowser" bind Method.GET to defineHandler { request, ipc ->
        debugBrowser("do openinbrowser", request.uri)
        val url = queryUrl(request)
        // openView(sessionId = sessionId, url = url)
        openBrowserWindow(ipc, url = url)
        return@defineHandler Response(Status.OK)
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
        uninstallWindow(ipc, search = search)
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

  private var winLock = Mutex(false)

  private suspend fun uninstallWindow(ipc: Ipc, search: String?) {
    winLock.withLock {
      win?.close(false)
    }
  }

  /**
   * 窗口是单例模式
   */
  private var win: WindowController? = null
  private suspend fun openBrowserWindow(ipc: Ipc, search: String? = null, url: String? = null) =
    winLock.withLock {
      if (win != null) {
        return@withLock win!!
      }

      // 打开安装窗口
      val win = createWindowAdapterManager.createWindow(
        WindowState(
          WindowConstants(
            owner = ipc.remote.mmid,
            provider = mmid,
            microModule = this
          )
        ).also {
          it.mode = WindowMode.MAXIMIZE
        })
      win.state.closeTip =
        win.manager?.state?.activity?.resources?.getString(R.string.browser_confirm_to_close)
          ?: ""
      this.win = win
      win.onClose {
        winLock.withLock {
          this@BrowserNMM.win = null
        }
      }
      // 由于 WebView创建需要在主线程，所以这边做了 withContext 操作
      withContext(mainAsyncExceptionHandler) {
        browserController =
          BrowserController(win, this@BrowserNMM, browserServer).also { controller ->

            search?.let { controller.updateDWSearch(it) }
            url?.let { controller.updateDWUrl(it) }
          }
      }
      return win
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