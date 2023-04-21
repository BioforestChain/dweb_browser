package info.bagen.dwebbrowser.ui.browser.ios

import android.util.Log
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SwipeableDefaults
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
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
  ExperimentalMaterialApi::class
) constructor(
  val browserViewList: MutableList<BrowserBaseView> = mutableStateListOf(),
  val currentBrowserBaseView: MutableState<BrowserBaseView>,
  val pagerStateContent: PagerState = PagerState(0), // 用于表示展示内容
  val pagerStateNavigator: PagerState = PagerState(0), // 用于表示下面搜索框等内容
  val hotLinkList: MutableList<HotspotInfo> = mutableStateListOf(),
  val popupViewState: MutableState<PopupViewSate> = mutableStateOf(PopupViewSate.Options),
  val myInstallApp: MutableList<JmmMetadata> = mutableStateListOf(),
  val multiViewShow: MutableTransitionState<Boolean> = MutableTransitionState(false),
  val showBottomBar: MutableTransitionState<Boolean> = MutableTransitionState(true), // 用于网页上滑或者下滑时，底下搜索框和导航栏的显示
  val showSearchEngine: MutableTransitionState<Boolean> = MutableTransitionState(false), // 用于在输入内容后，显示本地检索以及提供搜索引擎
  val modalBottomSheetState: ModalBottomSheetState = ModalBottomSheetState(
    initialValue = ModalBottomSheetValue.Hidden, animationSpec = SwipeableDefaults.AnimationSpec,
    isSkipHalfExpanded = false, confirmValueChange = { true }), // 用于显示“选项”菜单
)

sealed class BrowserIntent {
  object ShowMainView : BrowserIntent()
  object WebViewGoBack : BrowserIntent()
  class UpdatePopupViewState(val state: PopupViewSate) : BrowserIntent()
  class UpdateCurrentBaseView(val currentPage: Int) : BrowserIntent()
  class UpdateBottomViewState(val show: Boolean) : BrowserIntent()
  class UpdateMultiViewState(val show: Boolean, val index: Int? = null) : BrowserIntent()
  class UpdateSearchEngineState(val show: Boolean) : BrowserIntent()
  class UpdateOptionScaffoldState(val show: Boolean) : BrowserIntent()
  class AddNewWebView(val url: String) : BrowserIntent()
  class SearchWebView(val url: String) : BrowserIntent()
  class RemoveBaseView(val id: Int) : BrowserIntent()
  class OpenDwebBrowser(val mmid: Mmid) : BrowserIntent()
  class SaveNewWebSiteInfo(val title: String?, val url: String?) : BrowserIntent()
}

class BrowserViewModel(val browserController: BrowserController) : ViewModel() {
  val uiState: BrowserUIState

  companion object {
    private var webviewId_acc = AtomicInteger(1)
  }

  init {
    val browserMainView = info.bagen.dwebbrowser.ui.entity.BrowserMainView()
    uiState = BrowserUIState(currentBrowserBaseView = mutableStateOf(browserMainView))
    uiState.browserViewList.add(browserMainView)
    viewModelScope.launch(ioAsyncExceptionHandler) {
      // loadHotInfo()
      async {// 挂在数据变化
        MutableStateFlow(JmmNMM.getAndUpdateJmmNmmApps()).collect {
          uiState.myInstallApp.clear()
          it.forEach { (_, value) ->
            uiState.myInstallApp.add(value.metadata)
          }
        }
      }
      async {

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
        is BrowserIntent.UpdateOptionScaffoldState -> {
          Log.e("lin.huang", "UpdateOptionScaffoldState ${action.show}")
          withContext(mainAsyncExceptionHandler) {
            if (action.show) {
              uiState.modalBottomSheetState.show()
            } else {
              uiState.modalBottomSheetState.hide()
            }
          }
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
        is BrowserIntent.SaveNewWebSiteInfo -> {
          action.url?.let {
            WebsiteDB.saveWebsiteInfo(WebSiteInfo(title = action.title ?: it, url = it))
          }
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