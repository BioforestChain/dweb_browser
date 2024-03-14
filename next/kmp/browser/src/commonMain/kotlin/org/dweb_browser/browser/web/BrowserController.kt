package org.dweb_browser.browser.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.web.data.BrowserStore
import org.dweb_browser.browser.web.data.KEY_NO_TRACE
import org.dweb_browser.browser.web.data.WebLinkManifest
import org.dweb_browser.browser.web.data.WebLinkStore
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.download.BrowserDownloadModel
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getWindow

class BrowserController(
  private val browserNMM: BrowserNMM,
  private val webLinkStore: WebLinkStore
) {
  private val browserStore = BrowserStore(browserNMM)

  private val closeWindowSignal = SimpleSignal()
  val onCloseWindow = closeWindowSignal.toListener()

  private val windowVisibleSignal = Signal<Boolean>()
  val onWindowVisible = windowVisibleSignal.toListener()

  private val addWebLinkSignal = Signal<WebLinkManifest>()
  val onWebLinkAdded = addWebLinkSignal.toListener()

  private var winLock = Mutex(false)

  val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  //  val searchEngines: MutableList<WebEngine> = mutableStateListOf()
  val bookmarks = MutableStateFlow<List<WebSiteInfo>>(listOf())
  val historyLinks: MutableMap<String, MutableList<WebSiteInfo>> = mutableStateMapOf()
  val historys = MutableStateFlow<Map<String, List<WebSiteInfo>>>(mapOf())
  var isNoTrace by mutableStateOf(false)

  init {
    ioAsyncScope.launch {
      isNoTrace = getStringFromStore(KEY_NO_TRACE)?.isNotEmpty() ?: false
      bookmarks.value = browserStore.getBookLinks()
      historys.value = browserStore.getHistoryLinks()
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
    historys.value += browserStore.getDaysHistoryLinks(offset)
  }

  suspend fun saveBookLinks() = browserStore.setBookLinks(bookmarks.value)

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

  val viewModel = BrowserViewModel(this, browserNMM)

  suspend fun openBrowserView(search: String? = null, url: String? = null, target: String? = null) =
    winLock.withLock {
      viewModel.openBrowserView(search, url, target)
    }

  /**
   * 浏览器添加webLink到桌面
   */
  suspend fun addUrlToDesktop(title: String, url: String, icon: String): Boolean {
    // 处理weblink icon
    val trimmedIcon = if ((icon.first() == '\"' && icon.last() == '\"')) {
      icon.substring(1, icon.length - 1)
    } else icon
    val responseIcon = browserNMM.nativeFetch(trimmedIcon)
    //判断能否访问到不行 就用默认的
    val icons = if (responseIcon.status != HttpStatusCode.OK) {
      listOf()
    } else {
      listOf(ImageResource(trimmedIcon))
    }
    val trimmedUrl = if ((url.first() == '\"' && url.last() == '\"')) {
      url.substring(1, url.length - 1)
    } else url
    val linkId = WebLinkManifest.createLinkId(trimmedUrl)
    val webLinkManifest =
      WebLinkManifest(id = linkId, title = title, url = trimmedUrl, icons = icons)
    // 先判断是否存在，如果存在就不重复执行
    if (webLinkStore.get(linkId) == null) {
      addWebLinkSignal.emit(webLinkManifest)
      return true
    }
    return false
  }

  suspend fun saveStringToStore(key: String, data: String) = browserStore.saveString(key, data)
  suspend fun getStringFromStore(key: String) = browserStore.getString(key)

  val downloadModel = BrowserDownloadModel(browserNMM)

  /**
   * 打开BottomSheetModal
   */
  suspend fun openDownloadView(args: WebDownloadArgs) {
    downloadModel.openDownloadView(args)
  }
}