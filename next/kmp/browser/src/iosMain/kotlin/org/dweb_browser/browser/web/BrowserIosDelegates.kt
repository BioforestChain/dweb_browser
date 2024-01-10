package org.dweb_browser.browser.web

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.data.WebSiteType
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.NSDataHelper.toByteArray
import org.dweb_browser.helper.platform.NSDataHelper.toNSData
import platform.Foundation.NSData
import platform.UIKit.UIImage
import org.dweb_browser.helper.platform.ios_browser.WebBrowserViewDelegateProtocol
import org.dweb_browser.helper.platform.ios_browser.WebBrowserViewDataSourceProtocol
import org.dweb_browser.helper.platform.ios_browser.WebBrowserViewDataProtocolProtocol
import platform.Foundation.NSError
import platform.posix.int32_t
import platform.posix.int64_t
import org.dweb_browser.helper.platform.DeepLinkHook.Companion.deepLinkHook
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import platform.UIKit.UIScreen
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class BrowserIosDelegate(var browserViewModel: BrowserViewModel? = null):  NSObject(), WebBrowserViewDelegateProtocol {

  private val scope = CoroutineScope(ioAsyncExceptionHandler)

  override fun createDesktopLinkWithLink(
    link: String,
    title: String,
    iconString: String,
    completionHandler: (NSError?) -> Unit
  ) {
    scope.launch {
      browserViewModel?.addUrlToDesktop(title, link, iconString)
      completionHandler(null)
    }
  }

  override fun openDeepLinkWithUrl(url: String) {
    deepLinkHook.emitOnInit(url)
  }

  override fun recognizedScreenGestures() {
    nativeViewController.emitOnGoBack()
  }

  override fun doActionWithName(name: String, params: Map<Any?, *>?) {
    // kmp与iOS快速调试代码调用点。
  }

}


@OptIn(ExperimentalForeignApi::class)
public class BrowserIosDataSource(var browserViewModel: BrowserViewModel? = null): NSObject(), WebBrowserViewDataSourceProtocol {

  private val scope = CoroutineScope(ioAsyncExceptionHandler)

  //region track mode

  public override fun setTrackModel(trackModel: Boolean) {
    saveTrackModel(trackModel)
  }

  private fun saveTrackModel(trackModel: Boolean) {
    scope.launch {
      browserViewModel?.saveBrowserMode(trackModel)
    }
  }

  public override fun trackModel(): Boolean = browserViewModel?.isNoTrace?.value ?: false

  //endregion


  // region history
  public override fun loadHistorys(): Map<Any?, List<*>>? {
    val resutl = browserViewModel?.getHistoryLinks()?.toMap()
    return resutl as Map<Any?, List<WebBrowserViewDataProtocolProtocol>>
  }

  public override fun loadMoreHistoryWithOff(off: int32_t, completionHandler: (NSError?) -> Unit) {
    scope.launch {
      browserViewModel?.loadMoreHistory(off)
      completionHandler(null)
    }
  }

  public override fun addHistoryWithTitle(
    title: String,
    url: String,
    icon: NSData?,
    completionHandler: (NSError?) -> Unit
  ) {
      scope.launch {
        val webSiteInfo = WebSiteInfo(title = title, url = url, icon = icon?.toByteArray(), type = WebSiteType.History)
        browserViewModel?.addHistoryLink(webSiteInfo)
        completionHandler(null)
      }
  }

  public override fun removeHistoryWithHistory(history: int64_t, completionHandler: (NSError?) -> Unit) {
    scope.launch {
      browserViewModel?.let { vm ->
        try {
          val del = vm.getHistoryLinks().flatMap {
            it.value
          }.first {
            it.id == history
          }
          vm.removeHistoryLink(del)
        } catch (e: NoSuchElementException) {
          println("not find")
        } finally {
          completionHandler(null)
        }
      }
    }
  }
  // endregion


  // region bookmark

  public override fun loadBookmarks(): kotlin.collections.List<*>? = browserViewModel?.getBookLinks()

  public override fun addBookmarkWithTitle(title: String, url: String, icon: NSData?) {
    val webSiteInfo =
      WebSiteInfo(title = title, url = url, icon = icon?.toByteArray(), type = WebSiteType.Book)
    browserViewModel?.addBookLink(webSiteInfo)
  }

  public override fun removeBookmarkWithBookmark(bookmark: int64_t) {
    browserViewModel?.let { vm ->
      val del = vm.getBookLinks().first {
        it.id == bookmark
      }
      vm.removeBookLink(del)
    }
  }

  public override fun getWebBrowserViewDataClass(): String {
    return "KMPWebSiteInfo"
  }

  public override fun getDatasFor(for_: String, params: Map<Any?, *>?): Map<Any?, *>? {
    // kmp与iOS快速调试代码调用点。
    return null
  }

  override fun getIconUIImageWithData(data: WebBrowserViewDataProtocolProtocol): UIImage? {
    val webSiteInfo = data as WebSiteInfo
    return webSiteInfo.iconUIImage()
  }

  // endregion


  // region webview
  public override fun destroyWebViewWithWeb(web: objcnames.classes.WKWebView): kotlin.Unit {
    val webView = web as DWebViewEngine
    webView.destroy()
  }

  public override fun getWebView(): objcnames.classes.WKWebView {
    val engine = DWebViewEngine(
      UIScreen.mainScreen.bounds,
      browserViewModel!!.browserNMM,
      DWebViewOptions(),
      WKWebViewConfiguration()
    )
    return engine as objcnames.classes.WKWebView
  }

  //endregion
}

public fun WebSiteInfo.iconUIImage(): platform.UIKit.UIImage? {
  return icon?.let {
    return UIImage(data = it.toNSData())
  }
}