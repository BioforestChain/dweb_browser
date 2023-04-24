package info.bagen.dwebbrowser.ui.browser.ios

import android.content.Intent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import info.bagen.dwebbrowser.microService.sys.jmm.JmmNMM
import info.bagen.dwebbrowser.microService.webview.DWebView
import info.bagen.dwebbrowser.ui.entity.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.jsoup.Jsoup
import java.util.concurrent.atomic.AtomicInteger

data class BrowserUIState @OptIn(
  ExperimentalFoundationApi::class,
  ExperimentalMaterial3Api::class
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
)

sealed class BrowserIntent {
  object ShowMainView : BrowserIntent()
  object WebViewGoBack : BrowserIntent()
  class UpdatePopupViewState(val state: PopupViewState) : BrowserIntent()
  class UpdateCurrentBaseView(val currentPage: Int) : BrowserIntent()
  class UpdateBottomViewState(val show: Boolean) : BrowserIntent()
  class UpdateMultiViewState(val show: Boolean, val index: Int? = null) : BrowserIntent()
  class UpdateSearchEngineState(val show: Boolean) : BrowserIntent()
  class AddNewWebView(val url: String) : BrowserIntent()
  class SearchWebView(val url: String) : BrowserIntent()
  class RemoveBaseView(val id: Int) : BrowserIntent()
  class OpenDwebBrowser(val mmid: Mmid) : BrowserIntent()
  class SaveHistoryWebSiteInfo(val title: String?, val url: String?) : BrowserIntent()
  object SaveBookWebSiteInfo : BrowserIntent() // 直接获取当前的界面来保存
  object ShareWebSiteInfo : BrowserIntent() // 直接获取当前的界面来保存
  class UpdateInputText(val text: String) : BrowserIntent()
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
    viewModelScope.launch(ioAsyncExceptionHandler) {
      // loadHotInfo() // 加载热点数据
      async {// 挂在数据变化
        MutableStateFlow(JmmNMM.getAndUpdateJmmNmmApps()).collect {
          uiState.myInstallApp.clear()
          it.forEach { (_, value) ->
            uiState.myInstallApp.add(value.metadata)
          }
        }
      }
      async {
        WebsiteDB.queryHistoryWebsiteInfoList().collect {
          uiState.historyWebsiteMap.clear()
          it.forEach { (key, value) ->
            uiState.historyWebsiteMap[WebsiteDB.compareWithLocalTime(key)] = value
          }
        }
      }
      async {
        WebsiteDB.queryBookWebsiteInfoList().collect {
          uiState.bookWebsiteList.clear()
          it.forEach { websiteInfo ->
            uiState.bookWebsiteList.add(websiteInfo)
          }
        }
      }
    }
  }

  @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
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
        is BrowserIntent.AddNewWebView -> {
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
              uiState.browserViewList.add(uiState.browserViewList.size - 1, item)
              uiState.currentBrowserBaseView.value = item
            }
          }
        }
        is BrowserIntent.OpenDwebBrowser -> {
          BrowserNMM.browserController.openApp(action.mmid)
        }
        is BrowserIntent.SearchWebView -> {
          uiState.currentBrowserBaseView.value.let { browserBaseView ->
            if (browserBaseView is BrowserWebView) {
              browserBaseView.state.content = WebContent.Url(action.url)
            }
          }
        }
        is BrowserIntent.RemoveBaseView -> {
          uiState.browserViewList.removeAt(action.id)
        }
        is BrowserIntent.SaveHistoryWebSiteInfo -> {
          action.url?.let {
            WebsiteDB.saveHistoryWebsiteInfo(
              WebSiteInfo(title = action.title ?: it, url = it),
              uiState.historyWebsiteMap[WebsiteDB.compareWithLocalTime(WebsiteDB.currentLocalTime)]
            )
          }
        }
        is BrowserIntent.SaveBookWebSiteInfo -> {
          uiState.currentBrowserBaseView.value.let {
            if (it is BrowserWebView) {
              WebsiteDB.saveBookWebsiteInfo(
                WebSiteInfo(title = it.state.pageTitle ?: "", url = it.state.lastLoadedUrl ?: ""),
                uiState.bookWebsiteList
              )
            }
          }
        }
        is BrowserIntent.ShareWebSiteInfo -> {
          uiState.currentBrowserBaseView.value.let {
            if (it is BrowserWebView) {
              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Intent.EXTRA_SUBJECT, "分享") // 分享主题
                putExtra(Intent.EXTRA_TEXT, it.state.lastLoadedUrl ?: "") // 分享内容
              }
              App.appContext.startActivity(Intent.createChooser(shareIntent, "分享"))
            }
          }
        }
        is BrowserIntent.UpdateInputText -> {
          uiState.inputText.value = action.text
        }
      }
    }
  }

  private suspend fun createDwebView(url: String): DWebView =
    withContext(mainAsyncExceptionHandler) {
      val dWebView = DWebView(
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
//        it.isDrawingCacheEnabled = true
      }
      dWebView
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