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
import org.dweb_browser.sys.toast.PositionType
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

  val viewModel = BrowserViewModel(this, browserNMM)
  private val browserStore = BrowserStore(browserNMM)

  private val addWebLinkSignal = Signal<WebLinkManifest>()
  val onWebLinkAdded = addWebLinkSignal.toListener()

  private var winLock = Mutex(false)

  val ioScope get() = browserNMM.mmScope

  //  val searchEngines: MutableList<WebEngine> = mutableStateListOf()
  val bookmarksStateFlow = MutableStateFlow<List<WebSiteInfo>>(listOf())
  val historyStateFlow = MutableStateFlow<Map<String, List<WebSiteInfo>>>(mapOf())

  init {
    ioScope.launch {
      bookmarksStateFlow.value = browserStore.getBookLinks()
      historyStateFlow.value = browserStore.getHistoryLinks()
//      val engines = browserStore.getSearchEngines()
//      if (engines.isNotEmpty()) {
//        // 下面判断是否在 DefaultSearchWebEngine 有新增，有新增内置，需要补充进去
//        val notExists = DefaultSearchWebEngine.filter { default ->
//          engines.find { engine -> default.host == engine.host } == null
//        }
//        if (notExists.isNotEmpty()) {
//          DefaultSearchWebEngine.forEach { default ->
//            engines.find { it.host == default.host }?.let { engine ->
//              searchEngines.add(engine)
//            } ?: searchEngines.add(default)
//          }
//          browserStore.setSearchEngines(searchEngines)
//        } else {
//          searchEngines.addAll(engines)
//        }
//      } else {
//        searchEngines.addAll(DefaultSearchWebEngine)
//        browserStore.setSearchEngines(searchEngines)
//      }
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

  val downloadController = BrowserDownloadController(browserNMM, this)

  /**
   * 打开BottomSheetModal
   */
  suspend fun openDownloadView(args: WebDownloadArgs) = downloadController.openDownloadView(args)

  fun showToastMessage(message: String, position: PositionType? = null) = ioScope.launch {
    browserNMM.showToast(message = message, position = position)
  }
}