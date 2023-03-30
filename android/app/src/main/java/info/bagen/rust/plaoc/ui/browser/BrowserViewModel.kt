package info.bagen.rust.plaoc.ui.browser

import android.graphics.Bitmap
import android.webkit.WebView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.helper.ioAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.helper.mainAsyncExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

data class BrowserUIState(
  val browserViewList: MutableList<BrowserBaseView> = mutableStateListOf(),
  val currentBrowserWebView: MutableState<BrowserWebView?> = mutableStateOf(null),
)

interface BrowserBaseView {
  val show: MutableState<Boolean>
  val focus: MutableState<Boolean>
}

data class BrowserMainView(
  override val show: MutableState<Boolean> = mutableStateOf(true),
  override val focus: MutableState<Boolean> = mutableStateOf(false),
  val aaa: String,
) : BrowserBaseView

data class BrowserWebView(
  override val show: MutableState<Boolean> = mutableStateOf(true),
  override val focus: MutableState<Boolean> = mutableStateOf(false),
  val webView: WebView,
  val webViewId: String,
  val state: WebViewState,
  val navigator: WebViewNavigator,
  val coroutineScope: CoroutineScope,
  val bitmap: Bitmap? = null
) : BrowserBaseView

sealed class BrowserIntent {
  object ShowMainView : BrowserIntent()
  object WebViewGoBack : BrowserIntent()
  class UpdateCurrentWebView(val currentPage: Int) : BrowserIntent()
  class AddNewWebView(val url: String) : BrowserIntent()
  class SearchWebView(val url: String) : BrowserIntent()
}

class BrowserViewModel : ViewModel() {
  val uiState = BrowserUIState()

  companion object {
    private var webviewId_acc = AtomicInteger(1)
  }

  init {
    uiState.browserViewList.add(BrowserMainView(aaa = "主页啦"))
  }

  fun handleIntent(action: BrowserIntent) {
    viewModelScope.launch(ioAsyncExceptionHandler) {
      when (action) {
        is BrowserIntent.ShowMainView -> {
          uiState.browserViewList.lastOrNull()?.also {
            it.show.value = true
          }
        }
        is BrowserIntent.WebViewGoBack -> {
          uiState.currentBrowserWebView.value?.navigator?.navigateBack()
        }
        is BrowserIntent.UpdateCurrentWebView -> {
          uiState.currentBrowserWebView.value =
            when (val item = uiState.browserViewList[action.currentPage]) {
              is BrowserWebView -> item
              else -> null
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
            val state = WebViewState(WebContent.Url(action.url ?: ""))
            val coroutineScope = CoroutineScope(CoroutineName(webviewId))
            val navigator = WebViewNavigator(coroutineScope)
            BrowserWebView(
              webViewId = webviewId,
              webView = WebView(App.appContext),
              state = state,
              coroutineScope = coroutineScope,
              navigator = navigator
            ).also { item ->
              uiState.browserViewList.add(uiState.browserViewList.size - 1, item)
              uiState.currentBrowserWebView.value = item
            }
          }
        }
        is BrowserIntent.SearchWebView -> {
          uiState.currentBrowserWebView.value?.let {
            it.state.content = WebContent.Url(action.url)
          }
        }
      }
    }
  }

  private fun createWebView() : WebView {
    return WebView(App.appContext)
  }
}