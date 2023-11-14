package org.dweb_browser.browser.web

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.web.model.WebLinkManifest
import org.dweb_browser.browser.web.ui.browser.model.BrowserStore
import org.dweb_browser.browser.web.ui.browser.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.browser.model.WebSiteInfo
import org.dweb_browser.core.std.http.HttpDwebServer
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
  private val browserNMM: BrowserNMM, private val browserServer: HttpDwebServer
) {
  private val browserStore = BrowserStore(browserNMM)

  private val closeWindowSignal = SimpleSignal()
  val onCloseWindow = closeWindowSignal.toListener()

  private val addWebLinkSignal = Signal<WebLinkManifest>()
  val onWebLinkAdded = addWebLinkSignal.toListener()

  private var winLock = Mutex(false)

  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  val bookLinks: MutableList<WebSiteInfo> = mutableListOf()
  val historyLinks: MutableMap<String, MutableList<WebSiteInfo>> = mutableMapOf()

  init {
    ioAsyncScope.launch {
      browserStore.getBookLinks().forEach { webSiteInfo ->
        bookLinks.add(webSiteInfo)
      }
      browserStore.getHistoryLinks().forEach { (key, webSiteInfoList) ->
        historyLinks[key] = webSiteInfoList
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
      // 如果没有tab，那么创建一个新的
      // TODO 这里的tab应该从存储中恢复
      if (viewModel.currentTab == null) {
        viewModel.createNewTab()
      }
      /// 提供渲染适配
      windowAdapterManager.provideRender(wid) { modifier ->
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
//    .also {
//    val bottomSheetsController = browserNMM.createBottomSheets { modifier ->
//      Column(
//        modifier = modifier
//          .verticalScroll(rememberScrollState())
//          .background(Color.Green),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//      ) {
//        for (i in 1..100) {
//          Text(text = "hi~$i")
//        }
//      }
//    };
//    bottomSheetsController.open()
//    bottomSheetsController.onClose {
//      val alertController = browserNMM.createAlert("你好", "关闭了")
//      alertController.open()
//      alertController.onClose {
//        bottomSheetsController.destroy()
//        alertController.destroy()
//      }
//    }
//  }

  suspend fun openBrowserView(search: String? = null, url: String? = null) = winLock.withLock {
    viewModel.createNewTab(search, url)
  }

  val showLoading: MutableState<Boolean> = mutableStateOf(false)
  var viewModel = BrowserViewModel(this, browserNMM, browserServer) { mmid ->
    ioAsyncScope.launch {
      browserNMM.bootstrapContext.dns.open(mmid)
    }
  }

  suspend fun addUrlToDesktop(context: Context, title: String, url: String, icon: ImageBitmap?) {
    // 由于已经放弃了DataStore，所有这边改为直接走WebLinkStore
    val linkId = WebLinkManifest.createLinkId(url)
    val icons = icon?.let { WebLinkManifest.bitmapToImageResource(context, it) }?.let { listOf(it) }
      ?: emptyList()
    val webLinkManifest = WebLinkManifest(id = linkId, title = title, url = url, icons = icons)
    addWebLinkSignal.emit(webLinkManifest)
    // 先判断是否存在，如果存在就不重复执行
    /*if (DownloadDBStore.checkWebLinkNotExists(context, url)) {
      DownloadDBStore.saveWebLink(context, createDeskWebLink(context, title, url, icon))
    }*/
  }
}