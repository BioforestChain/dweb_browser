package info.bagen.rust.plaoc.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
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
import org.jsoup.Jsoup
import java.util.concurrent.atomic.AtomicInteger

data class BrowserUIState(
  val browserViewList: MutableList<BrowserBaseView> = mutableStateListOf(),
  val currentBrowserBaseView: MutableState<BrowserBaseView>,
  val hotLinkList: MutableList<WebSiteInfo> = mutableStateListOf(),
  val popupViewState: MutableState<PopupViewSate> = mutableStateOf(PopupViewSate.NULL),
)

interface BrowserBaseView {
  val show: MutableState<Boolean> // 用于首页是否显示遮罩
  val focus: MutableState<Boolean> // 用于搜索框显示的内容，根据是否聚焦来判断
  val showBottomBar: MutableState<Boolean> // 用于网页上滑或者下滑时，底下搜索框和导航栏的显示
}

data class BrowserMainView(
  override val show: MutableState<Boolean> = mutableStateOf(true),
  override val focus: MutableState<Boolean> = mutableStateOf(false),
  override val showBottomBar: MutableState<Boolean> = mutableStateOf(true),
  val aaa: String
) : BrowserBaseView

data class BrowserWebView(
  override val show: MutableState<Boolean> = mutableStateOf(true),
  override val focus: MutableState<Boolean> = mutableStateOf(false),
  override val showBottomBar: MutableState<Boolean> = mutableStateOf(true),
  val webView: WebView,
  val webViewId: String,
  val state: WebViewState,
  val navigator: WebViewNavigator,
  val coroutineScope: CoroutineScope,
  var bitmap: Bitmap? = null,
  var loaded: Boolean = false,
) : BrowserBaseView

data class WebSiteInfo(
  val id: Int = 0,
  val name: String,
  val webUrl: String,
  val iconUrl: String = "",
) {
  fun showHotText(): AnnotatedString {
    val color = when (id) {
      1 -> Color.Red
      2 -> Color(0xFFFF6C2D)
      3 -> Color(0xFFFF6C2D)
      else -> Color.LightGray
    }
    return buildAnnotatedString {
      withStyle(style = SpanStyle(color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)) {
        append("$id".padEnd(4, ' '))
      }
      withStyle(
        style = SpanStyle(
          color = Color.Black,
          fontSize = 16.sp
        )
      ) {
        append(name)
      }
    }
  }
}

enum class PopupViewSate {
  NULL, Options, BookList, HistoryList, Share
}

sealed class BrowserIntent {
  object ShowMainView : BrowserIntent()
  object WebViewGoBack : BrowserIntent()
  class UpdatePopupViewState(val state: PopupViewSate = PopupViewSate.NULL) : BrowserIntent()
  class UpdateCurrentWebView(val currentPage: Int) : BrowserIntent()
  class AddNewWebView(val url: String) : BrowserIntent()
  class SearchWebView(val url: String) : BrowserIntent()
}

class BrowserViewModel() : ViewModel() {
  val uiState: BrowserUIState

  companion object {
    private var webviewId_acc = AtomicInteger(1)
  }

  init {
    val browserMainView = BrowserMainView(aaa = "主页啦")
    uiState = BrowserUIState(currentBrowserBaseView = mutableStateOf(browserMainView))
    uiState.browserViewList.add(browserMainView)
    viewModelScope.launch(ioAsyncExceptionHandler) { loadHotInfo() }
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
          uiState.currentBrowserBaseView.value.let { browserBaseView ->
            if (browserBaseView is BrowserWebView) browserBaseView.navigator.navigateBack()
          }
        }
        is BrowserIntent.UpdatePopupViewState -> {
          if (uiState.popupViewState.value == PopupViewSate.NULL) {
            uiState.popupViewState.value = action.state
          } else {
            uiState.popupViewState.value = PopupViewSate.NULL
          }
        }
        is BrowserIntent.UpdateCurrentWebView -> {
          if (action.currentPage >= 0 && action.currentPage < uiState.browserViewList.size) {
            uiState.currentBrowserBaseView.value = uiState.browserViewList[action.currentPage]
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
              webView = MyWebView(App.appContext).also {
                it.settings.apply {
                  this.loadWithOverviewMode = true
                  this.javaScriptEnabled = true
                }
              },
              state = state,
              coroutineScope = coroutineScope,
              navigator = navigator
            ).also { item ->
              uiState.browserViewList.add(uiState.browserViewList.size - 1, item)
              uiState.currentBrowserBaseView.value = item
            }
          }
        }

        is BrowserIntent.SearchWebView -> {
          uiState.currentBrowserBaseView.value.let { browserBaseView ->
            if (browserBaseView is BrowserWebView) {
              browserBaseView.state.content = WebContent.Url(action.url)
            }
          }
        }
      }
    }
  }

  class MyWebView(context: Context) : WebView(context) {
    @SuppressLint("MissingSuperCall")
    override fun onDetachedFromWindow() {
      return
//      super.onDetachedFromWindow()
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
      uiState.hotLinkList.add(WebSiteInfo(count++, title, webUrl = path ?: ""))
    }
  }
}