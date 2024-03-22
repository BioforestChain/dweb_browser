package org.dweb_browser.browser.web

import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.data.AppBrowserTarget
import org.dweb_browser.browser.web.data.WebLinkStore
import org.dweb_browser.browser.web.model.WebLinkMicroModule
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
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.ext.onRenderer
import org.dweb_browser.sys.window.ext.openMainWindow

val debugBrowser = Debugger("browser")

/**
 * TODO 这个模块应该进一步抽象，从而共享给IOS侧
 */
class BrowserNMM : NativeMicroModule("web.browser.dweb", "Web Browser") {
  init {
    short_name = BrowserI18nResource.browser_short_name.text;
    dweb_deeplinks = listOf("dweb://search", "dweb://openinbrowser")
    categories = listOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))
    display = DisplayMode.Fullscreen

    /// 提供图标文件的适配器。注意，这里不需要随着 BrowserNMM bootstrap 来安装，而是始终有效。
    /// 因为只要存在 BrowserNMM 这个模块，那么就会有桌面链接图标
    nativeFetchAdaptersManager.append { fromMM, request ->
      return@append request.respondLocalFile {
        if (filePath.startsWith("/web_icons/")) {
          debugBrowser("IconFile", "$fromMM => ${request.href}")
          returnFile(getImageResourceRootPath(), filePath.substring("/web_icons/".length))
        } else returnNext()
      }
    }
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val webLinkStore = WebLinkStore(this)
    val browserController = // 由于 WebView创建需要在主线程，所以这边做了 withContext 操作
      withMainContext {
        BrowserController(this@BrowserNMM, webLinkStore)
      }
    loadWebLinkApps(browserController, webLinkStore)

    onRenderer {
      browserController.renderBrowserWindow(wid)
    }
    val openBrowser = defineBooleanResponse {
      debugBrowser("do openinbrowser", request.href)
      request.queryOrNull("url")?.let { url ->
        openMainWindow()
        browserController.tryOpenBrowserPage(url = url,
          target = request.queryOrNull("target")?.let { AppBrowserTarget.valueOf(it) }
            ?: AppBrowserTarget.BLANK
        )
        true
      } ?: false
    }

    routes(
      "search" bindDwebDeeplink defineBooleanResponse {
        debugBrowser("do search", request.href)
        request.queryOrNull("q")?.let { url ->
          openMainWindow()
          browserController.tryOpenBrowserPage(url = url, target = AppBrowserTarget.SELF)
          true
        } ?: false
      },
      "openinbrowser" bindDwebDeeplink openBrowser,
      "/openinbrowser" bind PureMethod.GET by openBrowser,
      "/uninstall" bind PureMethod.GET by defineBooleanResponse {
        debugBrowser("do uninstall", request.href)
        val mmid = request.query("app_id")
        bootstrapContext.dns.uninstall(mmid) && webLinkStore.delete(mmid)
      }
    )
  }

  /**
   * 用来加载WebLink数据的，并且监听是否添加到桌面操作
   */
  private suspend fun loadWebLinkApps(
    browserController: BrowserController, webLinkStore: WebLinkStore
  ) {
    webLinkStore.getAll().map { (_, webLinkManifest) ->
      bootstrapContext.dns.install(WebLinkMicroModule(webLinkManifest))
    }
    browserController.onWebLinkAdded { webLinkManifest -> // 监听是否添加到桌面操作
      // TODO 先存储，再注入到dns
      webLinkStore.delete(webLinkManifest.id)
      webLinkStore.set(webLinkManifest.id, webLinkManifest)
      bootstrapContext.dns.query(webLinkManifest.id)?.let {
        bootstrapContext.dns.uninstall(webLinkManifest.id)
      }
      bootstrapContext.dns.install(WebLinkMicroModule(webLinkManifest))
    }
  }

  override suspend fun _shutdown() {
  }
}