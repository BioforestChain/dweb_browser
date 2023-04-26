package info.bagen.dwebbrowser.ui.browser.ios

import android.content.Intent
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.datastore.WebsiteDB
import info.bagen.dwebbrowser.microService.browser.BrowserController
import info.bagen.dwebbrowser.microService.browser.BrowserNMM
import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.helper.mainAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.webview.DWebView
import info.bagen.dwebbrowser.ui.entity.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.util.concurrent.atomic.AtomicInteger

data class BrowserUIState @OptIn(
  ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class
) constructor(
  val browserViewList: MutableList<BrowserBaseView> = mutableStateListOf(), // 多浏览器列表
  val historyWebsiteMap: MutableMap<String, MutableList<WebSiteInfo>> = mutableStateMapOf(), // 历史列表
  val bookWebsiteList: MutableList<WebSiteInfo> = mutableStateListOf(), // 书签列表
  val hotLinkList: MutableList<HotspotInfo> = mutableStateListOf(), // 热点列表， 目前已舍弃，后续可移除
  val currentBrowserBaseView: MutableState<BrowserBaseView>,
  val pagerStateContent: PagerState = PagerState(0), // 用于表示展示内容
  val pagerStateNavigator: PagerState = PagerState(0), // 用于表示下面搜索框等内容
  val popupViewState: MutableState<PopupViewState> = mutableStateOf(PopupViewState.Options),
  val myInstallApp: MutableList<JmmMetadata> = mutableStateListOf(),
  val multiViewShow: MutableTransitionState<Boolean> = MutableTransitionState(false),
  val showBottomBar: MutableTransitionState<Boolean> = MutableTransitionState(true), // 用于网页上滑或者下滑时，底下搜索框和导航栏的显示
  val showSearchEngine: MutableTransitionState<Boolean> = MutableTransitionState(false), // 用于在输入内容后，显示本地检索以及提供搜索引擎
  val modalBottomSheetState: SheetState = SheetState(skipPartiallyExpanded = false), // 用于显示“选项”菜单
  val openBottomSheet: MutableState<Boolean> = mutableStateOf(false), // 用于显示“选项”菜单
  val inputText: MutableState<String> = mutableStateOf(""), // 用于指定输入的内容
  val keyboardOpened: MutableState<Boolean> = mutableStateOf(false), // 用于判断键盘的状态
)

sealed class BrowserIntent {
  object ShowMainView : BrowserIntent()
  object WebViewGoBack : BrowserIntent()
  object AddNewMainView : BrowserIntent()
  class UpdatePopupViewState(val state: PopupViewState) : BrowserIntent()
  class UpdateCurrentBaseView(val currentPage: Int) : BrowserIntent()
  class UpdateBottomViewState(val show: Boolean) : BrowserIntent()
  class UpdateMultiViewState(val show: Boolean, val index: Int? = null) : BrowserIntent()
  class UpdateSearchEngineState(val show: Boolean) : BrowserIntent()
  class SearchWebView(val url: String) : BrowserIntent()
  class RemoveBaseView(val id: Int) : BrowserIntent()
  class OpenDwebBrowser(val mmid: Mmid) : BrowserIntent()
  class SaveHistoryWebSiteInfo(val title: String?, val url: String?) : BrowserIntent()
  object SaveBookWebSiteInfo : BrowserIntent() // 直接获取当前的界面来保存
  object ShareWebSiteInfo : BrowserIntent() // 直接获取当前的界面来保存
  class UpdateInputText(val text: String) : BrowserIntent()
  class KeyboardStateChanged(val pair: Pair<Boolean, Int>) : BrowserIntent() // 键盘显示与否
  class DeleteWebSiteList(
    val type: ListType, val website: WebSiteInfo?, val clsAll: Boolean = false
  ) : BrowserIntent()
}

enum class ListType {
  History, Book
}

class BrowserViewModel(val browserController: BrowserController) : ViewModel() {
  val uiState: BrowserUIState

  companion object {
    private var webviewId_acc = AtomicInteger(1)
  }

  init {
    val browserMainView = BrowserMainView()
    uiState = BrowserUIState(currentBrowserBaseView = mutableStateOf(browserMainView))
    uiState.browserViewList.add(browserMainView)
    /*viewModelScope.launch(ioAsyncExceptionHandler) {
      // loadHotInfo() // 加载热点数据
      // 挂在数据变化
      MutableStateFlow(JmmNMM.getAndUpdateJmmNmmApps()).collect {
        uiState.myInstallApp.clear()
        it.forEach { (_, value) ->
          uiState.myInstallApp.add(value.metadata)
        }
      }
    }*/
    viewModelScope.launch(ioAsyncExceptionHandler) {
      WebsiteDB.queryHistoryWebsiteInfoMap().collect {
        uiState.historyWebsiteMap.clear()
        var index = 0
        it.toSortedMap { o1, o2 ->
          if (o1 < o2) 1 else -1
        }.forEach { (key, value) ->
          value.forEach { webSiteInfo -> webSiteInfo.index = index++ }
          uiState.historyWebsiteMap[key] = value
        }
      }
    }
    viewModelScope.launch(ioAsyncExceptionHandler) {
      WebsiteDB.queryBookWebsiteInfoList().collect { list ->
        uiState.bookWebsiteList.clear()
        list.forEach {
          uiState.bookWebsiteList.add(it)
        }
      }
    }
  }

  @OptIn(ExperimentalFoundationApi::class)
  fun handleIntent(action: BrowserIntent) {
    viewModelScope.launch(ioAsyncExceptionHandler) {
      when (action) {
        is BrowserIntent.ShowMainView -> {
          uiState.browserViewList.lastOrNull()?.also {
            it.show.value = true
          }
        }
        is BrowserIntent.WebViewGoBack -> {
          uiState.currentBrowserBaseView.value.let { browserBaseView ->
            if (browserBaseView is BrowserWebView) browserBaseView.navigator.navigateBack()
          }
        }
        is BrowserIntent.UpdatePopupViewState -> {
          uiState.popupViewState.value = action.state
        }
        is BrowserIntent.UpdateCurrentBaseView -> {
          if (action.currentPage >= 0 && action.currentPage < uiState.browserViewList.size) {
            uiState.currentBrowserBaseView.value = uiState.browserViewList[action.currentPage]
          }
        }
        is BrowserIntent.UpdateBottomViewState -> {
          uiState.showBottomBar.targetState = action.show
        }
        is BrowserIntent.UpdateMultiViewState -> {
          if (action.show) {
            uiState.currentBrowserBaseView.value.controller.capture()
          }
          uiState.multiViewShow.targetState = action.show
          action.index?.let {
            viewModelScope.launch(mainAsyncExceptionHandler) {
              uiState.pagerStateNavigator.scrollToPage(it)
              uiState.pagerStateContent.scrollToPage(it)
            }
          }
        }
        is BrowserIntent.UpdateSearchEngineState -> {
          uiState.showSearchEngine.targetState = action.show
        }
        is BrowserIntent.AddNewMainView -> {
          val itemView = BrowserMainView()
          uiState.browserViewList.add(itemView)
          uiState.currentBrowserBaseView.value = itemView
          viewModelScope.launch(mainAsyncExceptionHandler) {
            delay(100)
            uiState.multiViewShow.targetState = false
            uiState.pagerStateNavigator.scrollToPage(uiState.browserViewList.size - 1)
            uiState.pagerStateContent.scrollToPage(uiState.browserViewList.size - 1)
          }
        }
        is BrowserIntent.SearchWebView -> {
          uiState.showSearchEngine.targetState = false // 到搜索功能了，搜索引擎必须关闭
          when (val itemView = uiState.currentBrowserBaseView.value) {
            is BrowserWebView -> {
              itemView.state.content = WebContent.Url(action.url)
            }
            else -> {
              // 新增后，将主页界面置为 false，当搜索框右滑的时候，再重新置为 true
              uiState.browserViewList.lastOrNull()?.let {
                it.show.value = false
              }
              // 创建 webview 并且打开
              withContext(mainAsyncExceptionHandler) {
                val webviewId = "#web${webviewId_acc.getAndAdd(1)}"
                val state = WebViewState(WebContent.Url(action.url))
                val coroutineScope = CoroutineScope(CoroutineName(webviewId))
                val navigator = WebViewNavigator(coroutineScope)
                BrowserWebView(
                  webViewId = webviewId,
                  webView = createDwebView(action.url),
                  state = state,
                  coroutineScope = coroutineScope,
                  navigator = navigator
                ).also { item ->
                  for (index in 0 until uiState.browserViewList.size) {
                    val itemView = uiState.browserViewList[index]
                    if (itemView === uiState.currentBrowserBaseView.value) {
                      uiState.browserViewList.removeAt(index)
                      uiState.browserViewList.add(index, item)
                      uiState.currentBrowserBaseView.value = item
                      break
                    }
                  }
                  // uiState.browserViewList.add(uiState.browserViewList.size - 1, item)
                  // uiState.currentBrowserBaseView.value = item
                }
              }
            }
          }
        }
        is BrowserIntent.OpenDwebBrowser -> {
          BrowserNMM.browserController.openApp(action.mmid)
        }
        is BrowserIntent.RemoveBaseView -> {
          uiState.browserViewList.removeAt(action.id)
        }
        is BrowserIntent.SaveHistoryWebSiteInfo -> {
          action.url?.let {
            WebsiteDB.saveHistoryWebsiteInfo(WebSiteInfo(title = action.title ?: it, url = it))
          }
        }
        is BrowserIntent.SaveBookWebSiteInfo -> {
          uiState.currentBrowserBaseView.value.let {
            if (it is BrowserWebView) {
              WebsiteDB.saveBookWebsiteInfo(
                WebSiteInfo(title = it.state.pageTitle ?: "", url = it.state.lastLoadedUrl ?: "")
              )
            }
          }
        }
        is BrowserIntent.ShareWebSiteInfo -> {
          uiState.currentBrowserBaseView.value.let {
            if (it is BrowserWebView) {
              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, it.state.lastLoadedUrl ?: "") // 分享内容
                // putExtra(Intent.EXTRA_SUBJECT, "分享标题")
                putExtra(Intent.EXTRA_TITLE, it.state.pageTitle) // 分享标题
              }
              browserController.activity?.startActivity(Intent.createChooser(shareIntent, "分享到"))
            }
          }
        }
        is BrowserIntent.UpdateInputText -> {
          uiState.inputText.value = action.text
        }
        is BrowserIntent.KeyboardStateChanged -> {
          uiState.keyboardOpened.value = action.pair.first
        }
        is BrowserIntent.DeleteWebSiteList -> {
          when (action.type) {
            ListType.Book -> {
              if (action.clsAll) {
                WebsiteDB.clearBookWebsiteInfo()
              } else {
                action.website?.let { item -> WebsiteDB.deleteBookWebsiteInfo(item) }
              }
            }
            ListType.History -> {
              if (action.clsAll) {
                WebsiteDB.clearHistoryWebsiteInfo()
              } else {
                action.website?.let { WebsiteDB.deleteHistoryWebsiteInfo(it) }
              }
            }
          }
        }
      }
    }
  }

  private suspend fun createDwebView(url: String): DWebView =
    withContext(mainAsyncExceptionHandler) {
      DWebView(
        App.appContext,
        browserController.browserNMM,
        browserController.browserNMM,
        DWebView.Options(
          url = url,
          /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
          onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
        ),
        null
      ).also {
        it.webViewClient = DwebBrowserWebViewClient()
      }
    }

  private suspend fun loadHotInfo() {
    // val hotLink = "https://www.sinovision.net/portal.php?mod=center"
    val hotLink = "https://top.baidu.com/board?tab=realtime"
    // 加载全网热搜
    var doc = Jsoup.connect(hotLink).ignoreHttpErrors(true).get()
    var elementContent = doc.getElementsByClass("content_1YWBm")
    var count = 1
    elementContent.forEach { element ->
      if (count > 10) return@forEach
      val title = element.getElementsByClass("c-single-text-ellipsis").text()
      val path = element.select("a").first()?.attr("href")
      uiState.hotLinkList.add(HotspotInfo(count++, title, webUrl = path ?: ""))
    }
  }
}

internal class DwebBrowserWebViewClient : AccompanistWebViewClient() {
  override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      request?.url?.let { uri ->
        val url = uri.toString()
        if (url.startsWith("http") || url.startsWith("file") || url.startsWith("ftp")) {
          return super.shouldOverrideUrlLoading(view, request)
        }
        // 暂时不跳转
        /*try {
          App.appContext.startActivity(Intent(Intent.ACTION_VIEW, request.url).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
          })
        } catch (_: Exception) {
        }*/
      }
      return true
    }
    return super.shouldOverrideUrlLoading(view, request)
  }

  override fun onReceivedError(
    view: WebView?,
    request: WebResourceRequest?,
    error: WebResourceError?
  ) {
    // super.onReceivedError(view, request, error)
    if (error?.errorCode == -2) { // net::ERR_NAME_NOT_RESOLVED
      val param = request?.url?.let { uri -> "?text=${uri.host}${uri.path}" } ?: ""
      view?.loadUrl("file:///android_asset/error.html$param")
    }
  }
}