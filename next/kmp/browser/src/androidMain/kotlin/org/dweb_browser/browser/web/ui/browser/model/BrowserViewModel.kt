package org.dweb_browser.browser.web.ui.browser.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.util.KEY_LAST_SEARCH_KEY
import org.dweb_browser.browser.web.util.KEY_NO_TRACE
import org.dweb_browser.browser.web.util.getBoolean
import org.dweb_browser.browser.web.util.saveBoolean
import org.dweb_browser.browser.web.util.saveString
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.base.DWebViewItem
import org.dweb_browser.dwebview.base.ViewItem
import org.dweb_browser.dwebview.closeWatcher.CloseWatcher
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.build
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.withMainContext
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalFoundationApi::class)
data class BrowserUIState(
  val currentBrowserBaseView: MutableState<BrowserWebView?> = mutableStateOf(null),
  val browserViewList: MutableList<BrowserWebView> = mutableStateListOf(), // 多浏览器列表
  val pagerStateContent: MutableState<PagerState?> = mutableStateOf(null), // 用于表示展示内容
  val pagerStateNavigator: MutableState<PagerState?> = mutableStateOf(null), // 用于表示下面搜索框等内容
  val multiViewShow: MutableTransitionState<Boolean> = MutableTransitionState(false),
  val showBottomBar: MutableTransitionState<Boolean> = MutableTransitionState(true), // 用于网页上滑或者下滑时，底下搜索框和导航栏的显示
  val inputText: MutableState<String> = mutableStateOf(""), // 用于指定输入的内容
  val showSearchEngine: MutableTransitionState<Boolean> = MutableTransitionState(false), // 用于在输入内容后，显示本地检索以及提供搜索引擎
  // val qrCodeScanState: QRCodeScanState = QRCodeScanState(), // 用于判断桌面的显示隐藏 // 暂时屏蔽qrCode
) {
  suspend fun focusBrowserView(view: BrowserWebView) = withMainContext {
    val index = browserViewList.indexOf(view);
    currentBrowserBaseView.value = view
    multiViewShow.targetState = false
    pagerStateNavigator.value?.scrollToPage(index)
    pagerStateContent.value?.scrollToPage(index)
  }
}

/**
 * 用于指定输入的内容
 */
val LocalInputText = compositionLocalOf {
  mutableStateOf("")
}

/**
 * 用于显示搜索的界面，也就是点击搜索框后界面
 */
val LocalShowSearchView = compositionLocalOf {
  mutableStateOf(false)
}

fun noLocalProvidedFor(name: String): Nothing {
  error("CompositionLocal $name not present")
}

val LocalShowIme = compositionLocalOf {
  mutableStateOf(false)
}

val LocalWebViewInitialScale = compositionLocalOf<Int> {
  noLocalProvidedFor("WebViewInitialScale")
}

sealed class BrowserIntent {
  data object ShowMainView : BrowserIntent()
  data object WebViewGoBack : BrowserIntent()
  class AddNewMainView(val url: String? = null) : BrowserIntent()
  class UpdateCurrentBaseView(val currentPage: Int) : BrowserIntent()
  class UpdateBottomViewState(val show: Boolean) : BrowserIntent()
  class UpdateMultiViewState(val show: Boolean, val index: Int? = null) : BrowserIntent()
  class UpdateSearchEngineState(val show: Boolean) : BrowserIntent()
  class SearchWebView(val url: String) : BrowserIntent()
  class RemoveBaseView(val id: Int) : BrowserIntent()
  class OpenDwebBrowser(val mmid: MMID) : BrowserIntent()
  class ShareWebSiteInfo(val activity: Activity) : BrowserIntent() // 直接获取当前的界面来保存
  class UpdateInputText(val text: String) : BrowserIntent()
  class ShowSnackbarMessage(val message: String, val actionLabel: String? = null) : BrowserIntent()
}

@OptIn(ExperimentalFoundationApi::class)
class BrowserViewModel(
  private val browserController: BrowserController,
  private val browserNMM: NativeMicroModule,
  private val browserServer: HttpDwebServer,
  val onOpenDweb: (MMID) -> Unit
) : ViewModel() {
  val uiState: BrowserUIState = BrowserUIState()
  val dwebLinkSearch: MutableState<String> = mutableStateOf("")

  companion object {
    private var webviewId_acc = AtomicInteger(1)
  }

  init {
    browserController.onCloseWindow {
      withMainContext {
        uiState.browserViewList.forEach { webview ->
          webview.viewItem.webView.destroy()
        }
        uiState.browserViewList.clear()
      }
    }
  }

  val currentTab get() = uiState.currentBrowserBaseView.value

  internal suspend fun createNewTab(search: String? = null, url: String? = null) {
    // 先判断search是否不为空，然后在判断search是否是地址，
    dwebLinkSearch.value = "" // 先清空搜索的内容
    if (search?.startsWith("dweb:") == true || url?.startsWith("dweb:") == true) {
      withMainContext {
        if (uiState.browserViewList.isEmpty()) {
          val item = getNewTabBrowserView()
          uiState.browserViewList.add(item)
          uiState.currentBrowserBaseView.value = item
        }
        handleIntent(BrowserIntent.SearchWebView(search ?: url ?: ""))
      }
    } else if (search?.isUrlOrHost() == true || url?.isUrlOrHost() == true) {
      handleIntent(BrowserIntent.AddNewMainView(search ?: url))
    } else {
      withMainContext {
        dwebLinkSearch.value = search ?: url ?: ""
        if (uiState.browserViewList.isEmpty()) {
          val item = getNewTabBrowserView()
          uiState.browserViewList.add(item)
          uiState.currentBrowserBaseView.value = item
        }
      }
    }
  }

  private fun getDesktopUrl() = browserServer.startResult.urlInfo.buildInternalUrl().build {
    resolvePath("/index.html")
    parameters["api-base"] = browserServer.startResult.urlInfo.buildPublicUrl().toString()
  }

  private suspend fun getNewTabBrowserView(url: String? = null): BrowserWebView {
    val (viewItem, closeWatcher) = appendWebViewAsItem(createDwebViewEngine())
    url?.let { viewItem.state.content = WebContent.Url(it) } // 初始化时，直接提供地址
    return BrowserWebView(
      viewItem = viewItem, closeWatcher = closeWatcher
    )
  }

  val searchBackBrowserView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    CoroutineScope(ioAsyncExceptionHandler).async {
      val (viewItem, closeWatcher) = appendWebViewAsItem(createDwebViewEngine())
      BrowserWebView(
        viewItem = viewItem, closeWatcher = closeWatcher
      )
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
          uiState.currentBrowserBaseView.value?.viewItem?.navigator?.navigateBack()
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
            uiState.currentBrowserBaseView.value?.controller?.capture()
          }
          uiState.multiViewShow.targetState = action.show
          action.index?.let {
            withMainContext {
              uiState.pagerStateNavigator.value?.scrollToPage(it)
              uiState.pagerStateContent.value?.scrollToPage(it)
            }
          }
        }

        is BrowserIntent.UpdateSearchEngineState -> {
          uiState.showSearchEngine.targetState = action.show
        }

        is BrowserIntent.AddNewMainView -> {
          withMainContext {
            val itemView = getNewTabBrowserView(action.url)
            uiState.browserViewList.add(itemView)
            uiState.focusBrowserView(itemView)
          }
        }

        is BrowserIntent.SearchWebView -> {
          uiState.showSearchEngine.targetState = false // 到搜索功能了，搜索引擎必须关闭
          val loadingState = uiState.currentBrowserBaseView.value?.loadState
          loadingState?.value = true
          if (action.url.startsWith("dweb:")) { // 负责拦截browser的dweb_deeplink
            browserNMM.nativeFetch(action.url)
            loadingState?.value = false
            return@launch
          } else {
            uiState.currentBrowserBaseView.value?.viewItem?.apply {
              state.content = WebContent.Url(url = action.url,
                additionalHttpHeaders = hashMapOf<String, String>().also { map ->
                  map["temp"] = System.currentTimeMillis().toString()
                } // 添加不同的 header 信息，会让WebView判定即使同一个url，也做新url处理
              )
            }
            loadingState?.value = false
          }
        }

        is BrowserIntent.OpenDwebBrowser -> {
          onOpenDweb(action.mmid)
        }

        is BrowserIntent.RemoveBaseView -> {
          uiState.browserViewList.removeAt(action.id).also {
            withMainContext {
              it.viewItem.webView.destroy() // 关闭后，需要释放掉
            }
          }
          if (uiState.browserViewList.size == 0) {
            withMainContext {
              getNewTabBrowserView().also {
                uiState.browserViewList.add(it)
                uiState.currentBrowserBaseView.value = it
                handleIntent(BrowserIntent.UpdateMultiViewState(false))
              }
            }
          }
        }

        is BrowserIntent.ShareWebSiteInfo -> {
          uiState.currentBrowserBaseView.value?.let {
            if (it.viewItem.state.lastLoadedUrl?.isSystemUrl() == true) {
              handleIntent(BrowserIntent.ShowSnackbarMessage("无效的分享"))
              return@let
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, it.viewItem.state.lastLoadedUrl ?: "") // 分享内容
              // putExtra(Intent.EXTRA_SUBJECT, "分享标题")
              putExtra(Intent.EXTRA_TITLE, it.viewItem.state.pageTitle) // 分享标题
            }
            action.activity.startActivity(Intent.createChooser(shareIntent, "分享到"))
          }
        }

        is BrowserIntent.UpdateInputText -> {
          uiState.inputText.value = action.text
        }

        is BrowserIntent.ShowSnackbarMessage -> {/*withMainContext {
            uiState.bottomSheetScaffoldState.snackbarHostState.showSnackbar(
              action.message, action.actionLabel
            )
          }*/
        }
      }
    }
  }


  private suspend fun createDwebViewEngine(url: String = "") = withMainContext {
    DWebViewEngine(
      browserNMM.getAppContext(), browserNMM, DWebViewOptions(
        url = url,
        /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
        onDetachedFromWindowStrategy = DWebViewOptions.DetachedFromWindowStrategy.Ignore,
      )
    )
  }

  private suspend fun appendWebViewAsItem(
    dWebView: DWebViewEngine,
    url: String? = null
  ): Pair<ViewItem, CloseWatcher> = withMainContext {
    val webviewId = "#w${webviewId_acc.getAndAdd(1)}"
    val state = WebViewState(WebContent.Url(url ?: getDesktopUrl().toString()))
    val coroutineScope = CoroutineScope(CoroutineName(webviewId))
    val navigator = WebViewNavigator(coroutineScope)
    val viewItem = DWebViewItem(
      webviewId = webviewId,
      webView = dWebView,
      state = state,
      coroutineScope = coroutineScope,
      navigator = navigator,
    )
    val closeWatcherController = CloseWatcher(viewItem)

    viewItem.webView.webChromeClient = object : WebChromeClient() {
      override fun onCreateWindow(
        view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
      ): Boolean {
        val transport = resultMsg.obj;
        if (transport is WebView.WebViewTransport) {
          viewItem.coroutineScope.launch {
            val dwebView = createDwebViewEngine()
            transport.webView = dwebView
            resultMsg.sendToTarget()

            // 它是有内部链接的，所以等到它ok了再说
            var mainUrl = dwebView.getUrlInMain()
            if (mainUrl?.isEmpty() != true) {
              dwebView.waitReady()
              mainUrl = dwebView.getUrlInMain()
            }

            /// 内部特殊行为，有时候，我们需要知道 isUserGesture 这个属性，所以需要借助 onCreateWindow 这个回调来实现
            /// 实现 CloseWatcher 提案 https://github.com/WICG/close-watcher/blob/main/README.md
            if (closeWatcherController.consuming.remove(mainUrl)) {
              val consumeToken = mainUrl!!
              closeWatcherController.apply(isUserGesture).also {
                withMainContext {
                  dwebView.destroy()
                  closeWatcherController.resolveToken(consumeToken, it)
                }
              }
            } else {
              /// 打开一个新窗口
              val (newViewItem, closeWatcher) = appendWebViewAsItem(dwebView, mainUrl)
              withMainContext {
                val browserWebView = BrowserWebView(
                  viewItem = newViewItem, closeWatcher = closeWatcher
                )
                if (uiState.browserViewList.add(browserWebView)) {
                  uiState.focusBrowserView(browserWebView)
                }
              }
            }
          }
          return true
        }
        return super.onCreateWindow(
          view, isDialog, isUserGesture, resultMsg
        )
      }
    }
    Pair(viewItem, closeWatcherController)
  }

  val isNoTrace = mutableStateOf(browserNMM.getAppContext().getBoolean(KEY_NO_TRACE, false))
  fun saveBrowserMode(noTrace: Boolean) {
    isNoTrace.value = noTrace
    browserNMM.getAppContext().saveBoolean(KEY_NO_TRACE, noTrace)
  }

  /**
   * 添加到桌面功能
   */
  suspend fun addUrlToDesktop(context: Context) {
    uiState.currentBrowserBaseView.value?.viewItem?.state?.let { state ->
      state.lastLoadedUrl?.let { url ->
        browserController.addUrlToDesktop(context, state.pageTitle ?: "无标题", url, state.pageIcon)
      }
    }
  }

  fun getBookLinks() = browserController.bookLinks
  fun getHistoryLinks() = browserController.historyLinks

  /**
   * 操作历史数据
   * 新增：需要新增数据
   * 修改：历史数据没有修改
   * 删除：需要删除数据
   */
  suspend fun changeHistoryLink(add: WebSiteInfo? = null, del: WebSiteInfo? = null) {
    add?.apply {
      if (isNoTrace.value) return // 如果是无痕模式，则不能进行存储历史操作
      val key = timeMillis.toString()
      browserController.historyLinks.getOrPut(key) {
        mutableListOf()
      }.apply {
        add(0, add)
        browserController.saveHistoryLinks(key, this)
      }
    }
    del?.apply {
      val key = timeMillis.toString()
      browserController.historyLinks[key]?.apply {
        remove(del)
        browserController.saveHistoryLinks(key, this)
      }
    }
  }

  /**
   * 操作书签数据
   * 新增：需要新增数据
   * 修改：该对象已经变更，可直接保存，所以不需要传
   * 删除：需要删除数据
   */
  suspend fun changeBookLink(add: WebSiteInfo? = null, del: WebSiteInfo? = null) {
    add?.apply {
      browserController.bookLinks.add(this)
    }
    del?.apply {
      browserController.bookLinks.remove(this)
    }
    browserController.saveBookLinks()
  }
}

/**
 * 将WebViewState转为WebSiteInfo
 */
fun WebViewState.toWebSiteInfo(type: WebSiteType): WebSiteInfo? {
  return this.lastLoadedUrl?.let { url ->
    if (!url.isSystemUrl()) { // 无痕模式，不保存历史搜索记录
      WebSiteInfo(
        title = pageTitle ?: url,
        url = url,
        type = type,
        //icon = pageIcon?.asImageBitmap()
      )
    } else null
  }
}

class BrowserViewModelHelper {
  companion object {
    fun saveLastKeyword(inputText: MutableState<String>, url: String) {
      NativeMicroModule.getAppContext().saveString(KEY_LAST_SEARCH_KEY, url)
      inputText.value = url
    }
  }
}

/**
 * 根据内容解析成需要显示的内容
 */
internal fun parseInputText(text: String, needHost: Boolean = true): String {
  val uri = Uri.parse(text)
  for (item in DefaultAllWebEngine) {
    if (item.fit(text)) return uri.getQueryParameter(item.queryName())!!
  }
  if (uri.scheme == "dweb" || uri.scheme == "about") {
    return text
  }
  return if (needHost && uri.host?.isNotEmpty() == true) {
    uri.host!!
  } else if (uri.getQueryParameter("text")?.isNotEmpty() == true) {
    uri.getQueryParameter("text")!!
  } else {
    text
  }
}

/**
 * 根据内容来判断获取引擎
 */
internal fun findWebEngine(url: String): WebEngine? {
  for (item in DefaultAllWebEngine) {
    if (item.fit(url)) return item
  }
  return null
}

/**
 * 判断输入内容是否是域名或者有效的网址
 */
internal fun String.isUrlOrHost(): Boolean {
  // 只判断 host(长度1~63,结尾是.然后带2~6个字符如[.com]，没有端口判断)：val regex = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}\$".toRegex()
  // 以 http 或者 https 或者 ftp 打头，可以没有
  // 字符串中只能包含数字和字母，同时可以存在-
  // 最后以 2~5个字符 结尾，可能还存在端口信息，端口信息限制数字，长度为1~5位
  val regex =
    "^((https?|ftp)://)?([a-zA-Z0-9]+([-.][a-zA-Z0-9]+)*\\.[a-zA-Z]{2,5}(:[0-9]{1,5})?(/.*)?)$".toRegex()
  val regex2 =
    "((https?|ftp)://)(((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}(:[0-9]{1,5})?(/.*)?)".toRegex()
  return regex.matches(this) || regex2.matches(this)
}

/**
 * 将输入的内容补充为网址，如果本身就是网址直接返回
 */
internal fun String.toRequestUrl(): String {
  return if (this.startsWith("http://") || this.startsWith("https://") || this.startsWith("ftp://")) {
    this
  } else {
    "https://$this"
  }
}

/**
 * 为了判断字符串是否是内置的地址
 */
internal fun String.isSystemUrl(): Boolean {
  return this.startsWith("file:///android_asset") || this.startsWith("chrome://") || this.startsWith(
    "about:"
  ) || this.startsWith("https://web.browser.dweb")
}
