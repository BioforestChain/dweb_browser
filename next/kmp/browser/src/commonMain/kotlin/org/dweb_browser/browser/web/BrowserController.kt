package org.dweb_browser.browser.web

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.web.model.BrowserStore
import org.dweb_browser.browser.web.model.KEY_NO_TRACE
import org.dweb_browser.browser.web.model.WebLinkManifest
import org.dweb_browser.browser.web.model.WebLinkStore
import org.dweb_browser.browser.web.model.WebSiteInfo
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.core.windowInstancesManager

class BrowserController(
  private val browserNMM: BrowserNMM,
  private val browserServer: HttpDwebServer,
  private val webLinkStore: WebLinkStore,
) {
  private val browserStore = BrowserStore(browserNMM)

  private val closeWindowSignal = SimpleSignal()
  val onCloseWindow = closeWindowSignal.toListener()

  private val windowVisibleSignal = Signal<Boolean>()
  val onWindowVisiable = windowVisibleSignal.toListener()

  private val addWebLinkSignal = Signal<WebLinkManifest>()
  val onWebLinkAdded = addWebLinkSignal.toListener()

  private var winLock = Mutex(false)

  val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  val bookLinks: MutableList<WebSiteInfo> = mutableStateListOf()
  val historyLinks: MutableMap<String, MutableList<WebSiteInfo>> = mutableStateMapOf()
  var isNoTrace: Boolean = false

  init {
    ioAsyncScope.launch {
      isNoTrace = getStringFromStore(KEY_NO_TRACE)?.isNotEmpty() ?: false
      browserStore.getBookLinks().forEach { webSiteInfo ->
        bookLinks.add(webSiteInfo)
      }
      browserStore.getHistoryLinks().forEach { (key, webSiteInfoList) ->
        historyLinks[key] = mutableStateListOf<WebSiteInfo>().also {
          it.addAll(webSiteInfoList)
        }
      }
      // TODO 遍历获取book的image
//      bookLinks.forEach { webSiteInfo ->
//        webSiteInfo.icon = LocalBitmapManager.loadImageBitmap(webSiteInfo.id)?.toByteArray()
//      }
    }
  }

  suspend fun loadMoreHistory(off: Int) {
    browserStore.getDaysHistoryLinks(off).forEach { (key, webSiteInfoList) ->
      if (historyLinks.keys.contains(key)) {
        val data = historyLinks.getOrPut(key) {
          mutableStateListOf()
        }.also {
          it.addAll(webSiteInfoList)
        }
        historyLinks[key] = data
      }
    }
  }

  suspend fun saveBookLinks() = browserStore.setBookLinks(bookLinks)

  suspend fun saveHistoryLinks(key: String, historyLinks: MutableList<WebSiteInfo>) =
    browserStore.setHistoryLinks(key, historyLinks)

  /**
   * 窗口是单例模式
   */
  private var win: WindowController? = null
  suspend fun renderBrowserWindow(wid: UUID) = winLock.withLock {
    (windowInstancesManager.get(wid) ?: throw Exception("invalid wid: $wid")).also { newWin ->
      if (win == newWin) {
        return@withLock
      }
      win = newWin
      newWin.state.apply {
        mode = WindowMode.MAXIMIZE
        setFromManifest(browserNMM)
      }
      /// 提供渲染适配
      windowAdapterManager.provideRender(wid) { modifier ->
        Render(modifier, this)
      }
      newWin.onVisible {
        windowVisibleSignal.emit(true)
      }
      newWin.onHidden {
        windowVisibleSignal.emit(false)
      }
      newWin.onClose {
        closeWindowSignal.emit()
        winLock.withLock {
          if (newWin == win) {
            win = null
          }
        }
      }
    }
  }

  var viewModel = BrowserViewModel(this, browserNMM, browserServer)

  suspend fun openBrowserView(search: String? = null, url: String? = null) = winLock.withLock {
    viewModel.openBrowserView(search, url)
  }

  /**
   * 浏览器添加webLink到桌面
   */
  suspend fun addUrlToDesktop(title: String, url: String, icon: String): Boolean {
    // 处理weblink icon
    var trimmedIcon = icon
    if ((icon.first() == '\"' && icon.last() == '\"')) {
      trimmedIcon = icon.substring(1, icon.length - 1)
    }
    val icons = ImageResource(trimmedIcon)

    var trimmedUrl = url
    if ((url.first() == '\"' && url.last() == '\"')) {
      trimmedUrl = url.substring(1, url.length - 1)
    }
    val linkId = WebLinkManifest.createLinkId(trimmedUrl)
    val webLinkManifest =
      WebLinkManifest(id = linkId, title = title, url = trimmedUrl, icons = listOf(icons))
    // 先判断是否存在，如果存在就不重复执行
    if (webLinkStore.get(linkId) == null) {
      addWebLinkSignal.emit(webLinkManifest)
      return true
    }
    return false
  }

  suspend fun saveStringToStore(key: String, data: String) = browserStore.saveString(key, data)
  suspend fun getStringFromStore(key: String) = browserStore.getString(key)
}