package org.dweb_browser.browser.web.ui.browser.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.util.isDeepLink
import org.dweb_browser.browser.util.isSystemUrl
import org.dweb_browser.browser.util.isUrlOrHost
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.debugBrowser
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
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.base.DWebViewItem
import org.dweb_browser.dwebview.create
import org.dweb_browser.dwebview.getIconBitmap
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
  suspend fun focusBrowserView(view: BrowserWebView) {
    val index = browserViewList.indexOf(view)
    currentBrowserBaseView.value = view
    multiViewShow.targetState = false
    debugBrowser("focusBrowserView", "index=$index, size=${browserViewList.size}")
    delay(100) // window没渲染，导致scroll操作没效果，所以这边增加点等待
    withMainContext {
      pagerStateNavigator.value?.scrollToPage(index)
      pagerStateContent.value?.scrollToPage(index)
    }
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
    if (search?.isDeepLink() == true || url?.isDeepLink() == true) {
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

  private suspend fun getNewTabBrowserView(url: String? = null) =
    createBrowserWebView(createDwebView(url))

  val searchBackBrowserView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    CoroutineScope(ioAsyncExceptionHandler).async {
      createBrowserWebView(createDwebView())
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
          uiState.currentBrowserBaseView.value?.viewItem?.webView?.goBack()
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
          if (action.url.isDeepLink()) { // 负责拦截browser的dweb_deeplink
            browserNMM.nativeFetch(action.url)
            loadingState?.value = false
            return@launch
          } else {
            uiState.currentBrowserBaseView.value?.viewItem?.apply {
              webView.loadUrl(action.url, true/* 强制加载 */)
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
            val url = it.viewItem.webView.getUrl()
            if (url.isSystemUrl()) {
              handleIntent(BrowserIntent.ShowSnackbarMessage("无效的分享"))
              return@let
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, url) // 分享内容
              // putExtra(Intent.EXTRA_SUBJECT, "分享标题")
              putExtra(Intent.EXTRA_TITLE, it.viewItem.webView.getTitle()) // 分享标题
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

        else -> null
      }
    }
  }


  private suspend fun createDwebView(url: String? = null) = IDWebView.create(
    browserNMM.getAppContext(), browserNMM, DWebViewOptions(
      url = url ?: "",
      /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
      detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
    )
  ).also { it.setVerticalScrollBarVisible(false) }

  private suspend fun createBrowserWebView(
    dWebView: IDWebView
  ): BrowserWebView = withMainContext {
    val webviewId = "#w${webviewId_acc.getAndAdd(1)}"
    val coroutineScope = CoroutineScope(CoroutineName(webviewId))
    val viewItem = DWebViewItem(
      webviewId = webviewId,
      webView = dWebView,
      coroutineScope = coroutineScope,
    )
    dWebView.onCreateWindow {
      val browserWebView = createBrowserWebView(it)
      if (uiState.browserViewList.add(browserWebView)) {
        uiState.focusBrowserView(browserWebView)
      }
    }

    BrowserWebView(viewItem)
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
    uiState.currentBrowserBaseView.value?.also { browserWebView ->
      val webView = browserWebView.viewItem.webView
      val url = webView.getUrl()
      browserController.addUrlToDesktop(
        context = context,
        title = webView.getTitle().ifEmpty { url },
        url = url,
        icon = webView.getIconBitmap()
      )
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
      val addUrl = this.url
      browserController.historyLinks.getOrPut(key) {
        mutableListOf()
      }.apply {
        removeIf { it.url == addUrl } // 删除同一天的重复数据
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

  suspend fun openQRCodeScanning() {
    val data = browserNMM.nativeFetch("file://barcode-scanning.sys.dweb/open").body.toPureString()
    // 如果是url，进行跳转，如果不是，就直接弹出对话框
    if (data.isNotEmpty()) {
      handleIntent(BrowserIntent.SearchWebView(data))
    }
  }
}

/**
 * 将WebViewState转为WebSiteInfo
 */
suspend fun IDWebView.toWebSiteInfo(type: WebSiteType): WebSiteInfo? {
  return this.getUrl().let { url ->
    if (!url.isSystemUrl()) { // 无痕模式，不保存历史搜索记录
      WebSiteInfo(
        title = getTitle().ifEmpty { url },
        url = url,
        type = type,
        icon = getIconBitmap()
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
