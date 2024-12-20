package org.dweb_browser.browser.web

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.web.data.AppBrowserTarget
import org.dweb_browser.browser.web.data.BrowserStore
import org.dweb_browser.browser.web.data.WebLinkManifest
import org.dweb_browser.browser.web.data.WebLinkStore
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isMobile
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.toast.ToastPositionType
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getWindow

class BrowserController(
  private val browserNMM: BrowserNMM.BrowserRuntime,
) {
  val webLinkStore: WebLinkStore = browserNMM.webLinkStore
  private val windowVisibleSignal = Signal<Boolean>()
  val onWindowVisible = windowVisibleSignal.toListener()

  private val closeWindowSignal = SimpleSignal()
  val onCloseWindow = closeWindowSignal.toListener()

  val downloadController = BrowserDownloadController(browserNMM, this)
  val viewModel = BrowserViewModel(this, browserNMM)
  private val browserStore = BrowserStore(browserNMM)

  private var winLock = Mutex(false)

  val lifecycleScope get() = browserNMM.getRuntimeScope()

  val bookmarksStateFlow = MutableStateFlow<List<WebSiteInfo>>(listOf())
  val historyStateFlow = MutableStateFlow<Map<String, List<WebSiteInfo>>>(mapOf())

  init {
    lifecycleScope.launch {
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

      /// 移动端默认最大化
      // TODO 这里应使用屏幕尺寸来判定
      if (IPureViewController.isMobile) {
        newWin.maximize()
      }

      /// 提供渲染适配
      windowAdapterManager.provideRender(wid) { modifier ->
        Render(modifier, this)

        // 不能直接将整个应用切换到后台，而是关闭当前应用
        win?.navigation?.GoBackHandler {
          win?.tryCloseOrHide()
        }
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
   * 将webLink迁移到desk，迁移完成后，本身数据进行删除
   */
  suspend fun loadWebLinkApps() {
    webLinkStore.getAll().map { (_, webLinkManifest) ->
      addUrlToDesktop(webLinkManifest.copy(
        url = buildUrlString("dweb://openinbrowser") {
          parameters["url"] = webLinkManifest.url
        }
      ))
    }
    webLinkStore.clear()
  }

  /**
   * 浏览器添加webLink到桌面
   */
  suspend fun addUrlToDesktop(title: String, url: String, icon: String): Boolean {
    val linkId = WebLinkManifest.createLinkId(url)
    val webLinkManifest = WebLinkManifest(
      id = linkId,
      title = title,
      url = buildUrlString("dweb://openinbrowser") { parameters["url"] = url },
      icons = listOf(ImageResource(icon, purpose = "maskable"))
    )

    return addUrlToDesktop(webLinkManifest)
  }

  suspend fun addUrlToDesktop(webLinkManifest: WebLinkManifest): Boolean {
    // TODO 直接调用 DesktopNMM 进行存储管理等
    return browserNMM.nativeFetch(
      PureClientRequest(
        href = "file://desk.browser.dweb/addWebLink",
        method = PureMethod.POST,
        body = IPureBody.from(Json.encodeToString(webLinkManifest))
      )
    ).boolean()
  }

  suspend fun saveStringToStore(key: String, data: String) = browserStore.saveString(key, data)
  suspend fun getStringFromStore(key: String) = browserStore.getString(key)

  /**
   * 打开BottomSheetModal
   */
  suspend fun openDownloadDialog(args: WebDownloadArgs) =
    downloadController.openDownloadDialog(args)

  fun showToastMessage(message: String, position: ToastPositionType? = null) =
    lifecycleScope.launch {
      browserNMM.showToast(message = message, position = position)
    }
}