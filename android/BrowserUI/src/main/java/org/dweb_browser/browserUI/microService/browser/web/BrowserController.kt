package org.dweb_browser.browserUI.microService.browser.web

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browserUI.R
import org.dweb_browser.browserUI.ui.browser.model.WebSiteInfo
import org.dweb_browser.browserUI.ui.browser.model.BrowserViewModel
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.core.sys.download.db.DownloadDBStore
import org.dweb_browser.core.sys.download.db.createDeskWebLink
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.core.createWindowAdapterManager
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.windowInstancesManager

class BrowserController(
  private val browserNMM: BrowserNMM, private val browserServer: HttpDwebServer
) {

  private val closeWindowSignal = SimpleSignal()
  val onCloseWindow = closeWindowSignal.toListener()

  private var winLock = Mutex(false)

  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  val bookLinks: MutableList<WebSiteInfo> = mutableListOf()
  val historyLinks: MutableMap<String, MutableList<WebSiteInfo>> = mutableMapOf()

  init {
    ioAsyncScope.launch {
      browserNMM.browserStore.getBookLinks().forEach { webSiteInfo ->
        bookLinks.add(webSiteInfo)
      }
      browserNMM.browserStore.getHistoryLinks().forEach { (key, webSiteInfoList) ->
        historyLinks[key] = webSiteInfoList
      }
    }
  }

  suspend fun saveBookLinks() = browserNMM.browserStore.setBookLinks(bookLinks)

  suspend fun saveHistoryLinks(key: String, historyLinks: MutableList<WebSiteInfo>) =
    browserNMM.browserStore.setHistoryLinks(key, historyLinks)

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
        closeTip =
          newWin.manager?.state?.viewController?.androidContext?.getString(R.string.browser_confirm_to_close) // TODO 这里改成 kmp 的 i18n 标准
            ?: ""
      }
      // 如果没有tab，那么创建一个新的
      // TODO 这里的tab应该从存储中恢复
      if (viewModel.currentTab == null) {
        viewModel.createNewTab()
      }
      /// 提供渲染适配
      createWindowAdapterManager.renderProviders[wid] = @Composable { modifier ->
        Render(modifier, this)
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

  suspend fun openBrowserView(search: String? = null, url: String? = null) =
    winLock.withLock {
      viewModel.createNewTab(search, url)
    }

  val showLoading: MutableState<Boolean> = mutableStateOf(false)
  var viewModel = BrowserViewModel(this, browserNMM, browserServer) { mmid ->
    ioAsyncScope.launch {
      browserNMM.bootstrapContext.dns.open(mmid)
    }
  }

  suspend fun addUrlToDesktop(context: Context, title: String, url: String, icon: Bitmap?) =
    DownloadDBStore.saveWebLink(context, createDeskWebLink(context, title, url, icon))
}