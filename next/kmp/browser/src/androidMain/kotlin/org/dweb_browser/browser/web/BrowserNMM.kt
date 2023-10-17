package org.dweb_browser.browser.web

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.bindDwebDeeplink
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.ext.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.dweb_browser.core.sys.dns.returnAndroidFile
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.window.core.onRenderer
import org.dweb_browser.sys.window.ext.openMainWindow

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
    browserServer = this.createBrowserWebServer()
    val browserController = // 由于 WebView创建需要在主线程，所以这边做了 withContext 操作
      withMainContext {
        BrowserController(
          this@BrowserNMM, browserServer
        )
      }

    onRenderer {
      browserController.renderBrowserWindow(wid)
    }

    routes(
      "search" bindDwebDeeplink defineEmptyResponse {
        debugBrowser("do search", request.href)
        browserController.openBrowserView(search = request.query("q"))
        openMainWindow()
      },
      "openinbrowser" bindDwebDeeplink defineEmptyResponse {
        debugBrowser("do openinbrowser", request.href)
        browserController.openBrowserView(url = request.query("url"))
        openMainWindow()
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