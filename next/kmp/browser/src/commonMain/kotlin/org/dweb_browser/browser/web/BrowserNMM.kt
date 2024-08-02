package org.dweb_browser.browser.web

import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.data.AppBrowserTarget
import org.dweb_browser.browser.web.data.WebLinkStore
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindDwebDeeplink
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.ext.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.ext.onRenderer
import org.dweb_browser.sys.window.ext.openMainWindow

val debugBrowser = Debugger("browser")

/**
 * TODO 这个模块应该进一步抽象，从而共享给IOS侧
 */
class BrowserNMM : NativeMicroModule("web.browser.dweb", "Web Browser") {
  init {
    short_name = BrowserI18nResource.browser_short_name.text
    dweb_deeplinks = listOf("dweb://search", "dweb://openinbrowser")
    categories = listOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
    icons = listOf(
      ImageResource(src = "file:///sys/browser-icons/$mmid.svg", type = "image/svg+xml")
    )
    display = DisplayMode.Fullscreen

    /// 提供图标文件的适配器。注意，这里不需要随着 BrowserNMM bootstrap 来安装，而是始终有效。
    /// 因为只要存在 BrowserNMM 这个模块，那么就会有桌面链接图标
    nativeFetchAdaptersManager.append(order = 5) { fromMM, request ->
      return@append request.respondLocalFile {
        if (filePath.startsWith("/web_icons/")) {
          debugBrowser("IconFile", "$fromMM => ${request.href}")
          returnFile(getImageResourceRootPath(), filePath.substring("/web_icons/".length))
        } else returnNext()
      }
    }
  }

  inner class BrowserRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {

    val webLinkStore = WebLinkStore(this)
    val browserController = BrowserController(this)
    override suspend fun _bootstrap() {
      // 用于修复iOS新版桌面如果启动之后直接使用browser会导致代理为启动
      dwebviewProxyPrepare()

      // 由于 WebView创建需要在主线程，所以这边做了 withContext 操作
      browserController.loadWebLinkApps()

      onRenderer {
        browserController.renderBrowserWindow(wid)
      }
      val openBrowser = defineBooleanResponse {
        debugBrowser("do openinbrowser", request.href)
        val url = request.queryOrNull("url") ?: return@defineBooleanResponse false
        openMainWindow()
        val target = request.queryOrNull("target")?.let { AppBrowserTarget.ALL[it] }
          ?: AppBrowserTarget.BLANK
        browserController.tryOpenBrowserPage(url = url, target = target)
        true
      }
      val searchBrowser = defineBooleanResponse {
        debugBrowser("do search", request.href)
        val url = request.queryOrNull("q") ?: return@defineBooleanResponse false
        openMainWindow()
        browserController.tryOpenBrowserPage(url = url, target = AppBrowserTarget.SELF)
        true
      }
      routes(
        "search" bindDwebDeeplink searchBrowser,
        "/search" bind PureMethod.GET by searchBrowser,
        "openinbrowser" bindDwebDeeplink openBrowser,
        "/openinbrowser" bind PureMethod.GET by openBrowser
      )
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = BrowserRuntime(bootstrapContext)
}