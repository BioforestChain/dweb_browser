package org.dweb_browser.browserUI.microService.browser.web

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.browserUI.microService.browser.link.WebLinkMicroModule
import org.dweb_browser.core.getAppContext
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.http.bindDwebDeeplink
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.std.dns.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.microservice.std.dns.nativeFetch
import org.dweb_browser.microservice.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.microservice.std.http.DwebHttpServerOptions
import org.dweb_browser.microservice.std.http.HttpDwebServer
import org.dweb_browser.microservice.std.http.createHttpDwebServer
import org.dweb_browser.microservice.sys.dns.returnAndroidFile
import org.dweb_browser.microservice.sys.download.db.AppType
import org.dweb_browser.microservice.sys.download.db.DeskAppInfo
import org.dweb_browser.microservice.sys.download.db.DownloadDBStore

val debugBrowser = Debugger("browser")

/**
 * TODO 这个模块应该进一步抽象，从而共享给IOS侧
 */
class BrowserNMM : NativeMicroModule("web.browser.dweb", "Web Browser") {
  init {
    short_name = "Browser";
    dweb_deeplinks = listOf("dweb://search", "dweb://openinbrowser")
    categories = listOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))

    /// 提供图标文件的适配器。注意，这里不需要随着 BrowserNMM bootstrap 来安装，而是始终有效。
    /// 因为只要存在 BrowserNMM 这个模块，那么就会有桌面链接图标
    nativeFetchAdaptersManager.append { fromMM, request ->
      return@append request.respondLocalFile {
        if (filePath.startsWith("/web_icons/")) {
          debugBrowser("IconFile", "$fromMM => ${request.href}")
          returnAndroidFile(
            getAppContext().filesDir.absolutePath + "/icons",
            filePath.substring("/web_icons/".length)
          )
        } else returnNext()
      }
    }
  }

  private lateinit var browserServer: HttpDwebServer

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    ioAsyncScope.launch {
      var preList = mutableListOf<DeskAppInfo>()
      DownloadDBStore.queryDeskAppInfoList(getAppContext())
        .collectLatest { list -> // TODO 只要datastore更新，这边就会实时更新
          debugBrowser("AppInfoDataStore", "size=${list.size}")
          list.map { deskAppInfo ->
            when (deskAppInfo.appType) {
              AppType.URL -> deskAppInfo.weblink?.let { deskWebLink ->
                preList.removeIf { preDeskAppInfo -> preDeskAppInfo.weblink?.id == deskWebLink.id }
                bootstrapContext.dns.install(WebLinkMicroModule(deskWebLink))
              }

              else -> {}
            }
          }
          /// 将剩余的应用卸载掉
          for (uninstallItem in preList) {
            uninstallItem.weblink?.deleteIconFile(getAppContext()) // 删除已下载的图标
            (uninstallItem.metadata?.id ?: uninstallItem.weblink?.id)?.let { uninstallId ->
              bootstrapContext.dns.uninstall(uninstallId)
            }
          }
          preList = list
        }
    }

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
        debugBrowser("do search", request.href)
        browserController.openBrowserWindow(search = request.query("q"))
        return@definePureResponse PureResponse(HttpStatusCode.OK)
      },
      "openinbrowser" bindDwebDeeplink definePureResponse {
        debugBrowser("do openinbrowser", request.href)
        browserController.openBrowserWindow(url = request.query("url"))
        return@definePureResponse PureResponse(HttpStatusCode.OK)
      },
      "/uninstall" bind HttpMethod.Get to definePureResponse {
        debugBrowser("uninstall", request.href)
        val mmid = request.queryOrNull("mmid")
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