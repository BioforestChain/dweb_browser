package org.dweb_browser.browser.web

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.web.data.AppBrowserTarget
import org.dweb_browser.browser.web.data.BrowserStore
import org.dweb_browser.browser.web.data.WebLinkManifest
import org.dweb_browser.browser.web.data.WebLinkStore
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.UUID
import org.dweb_browser.sys.toast.ToastPositionType
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getWindow

class BrowserController(
  private val browserNMM: BrowserNMM.BrowserRuntime, private val webLinkStore: WebLinkStore
) {
  private val windowVisibleSignal = Signal<Boolean>()
  val onWindowVisible = windowVisibleSignal.toListener()

  private val closeWindowSignal = SimpleSignal()
  val onCloseWindow = closeWindowSignal.toListener()

  val downloadController = BrowserDownloadController(browserNMM, this)
  val viewModel = BrowserViewModel(this, browserNMM)
  private val browserStore = BrowserStore(browserNMM)

  private val addWebLinkSignal = Signal<WebLinkManifest>()
  val onWebLinkAdded = addWebLinkSignal.toListener()

  private var winLock = Mutex(false)

  val ioScope get() = browserNMM.getRuntimeScope()

  val bookmarksStateFlow = MutableStateFlow<List<WebSiteInfo>>(listOf())
  val historyStateFlow = MutableStateFlow<Map<String, List<WebSiteInfo>>>(mapOf())

  init {
    ioScope.launch {
      bookmarksStateFlow.value = browserStore.getBookLinks()
      historyStateFlow.value = browserStore.getHistoryLinks()
    }
  }

  suspend fun loadMoreHistory(offset: Int) {
    historyStateFlow.value += browserStore.getDaysHistoryLinks(offset)
  }

  suspend fun saveBookLinks() = browserStore.setBookLinks(bookmarksStateFlow.value)

  suspend fun saveHistoryLinks(key: String, dayList: List<WebSiteInfo>) =
    browserStore.setHistoryLinks(key, dayList)

//  suspend fun saveSearchEngines() = browserStore.setSearchEngines(searchEngines)

  /**
   * 窗口是单例模式
   */
  private var win: WindowController? = null
  suspend fun renderBrowserWindow(wid: UUID) = winLock.withLock {
    browserNMM.getWindow(wid).also { newWin ->
      if (win == newWin) {
        return@withLock
      }
      viewModel.addNewPageUI() // 第一次渲染需要添加一个HomePage
      win = newWin
      newWin.setStateFromManifest(browserNMM)

      /// 提供渲染适配
      windowAdapterManager.provideRender(wid) { modifier ->
        Render(modifier, this)
      }
      newWin.onVisible {
        windowVisibleSignal.emit(true)
      }
      newWin.onHidden {
        viewModel.hideAllPanel()
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

  // 用于获取窗口的视图盒子，对android来说，可以通过这个试图盒子获取activity
  val viewBox get() = win?.viewBox

  /**
   * 通过 deeplink 来打开 web browser界面后，需要考虑是否加载
   */
  suspend fun tryOpenBrowserPage(url: String, target: AppBrowserTarget = AppBrowserTarget.SELF) {
    viewModel.openSearchPanelUI(url, target)
  }

  /**
   * 浏览器添加webLink到桌面
   */
  suspend fun addUrlToDesktop(title: String, url: String, icon: String): Boolean {
    val linkId = WebLinkManifest.createLinkId(url)
    val webLinkManifest =
      WebLinkManifest(id = linkId, title = title, url = url, icons = listOf(ImageResource(icon)))
    // 先判断是否存在，如果存在就不重复执行
    if (webLinkStore.get(linkId) == null) {
      addWebLinkSignal.emit(webLinkManifest)
      return true
    }
    return false
  }

  suspend fun saveStringToStore(key: String, data: String) = browserStore.saveString(key, data)
  suspend fun getStringFromStore(key: String) = browserStore.getString(key)

  /**
   * 打开BottomSheetModal
   */
  suspend fun openDownloadDialog(args: WebDownloadArgs) = downloadController.openDownloadDialog(args)

  fun showToastMessage(message: String, position: ToastPositionType? = null) = ioScope.launch {
    browserNMM.showToast(message = message, position = position)
  }
}