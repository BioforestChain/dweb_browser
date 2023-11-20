package org.dweb_browser.browser.web.ui.browser.model

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.ktor.http.Url
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.dweb_browser.browser.util.isDeepLink
import org.dweb_browser.browser.util.isSystemUrl
import org.dweb_browser.browser.util.isUrlOrHost
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.browser.web.model.BrowserWebView
import org.dweb_browser.browser.web.model.KEY_LAST_SEARCH_KEY
import org.dweb_browser.browser.web.model.KEY_NO_TRACE
import org.dweb_browser.browser.web.model.WebSiteInfo
import org.dweb_browser.browser.web.model.WebSiteType
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.base.DWebViewItem
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.build
import org.dweb_browser.helper.compose.noLocalProvidedFor
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.helper.withMainContext

/**
 * 用于显示搜索的界面，也就是点击搜索框后界面
 */
val LocalShowSearchView = compositionLocalOf {
  mutableStateOf(false)
}

val LocalWebViewInitialScale = compositionLocalOf<Float> {
  noLocalProvidedFor("WebViewInitialScale")
}

val LocalShowIme = compositionLocalOf {
  mutableStateOf(false)
}

/**
 * 用于指定输入的内容
 */
val LocalInputText = compositionLocalOf {
  mutableStateOf("")
}

data class BrowserPagerState @OptIn(ExperimentalFoundationApi::class) constructor(
  val pagerStateContent: PagerState, // 用于表示展示内容
  val pagerStateNavigator: PagerState, // 用于表示下面搜索框等内容
)

val LocalBrowserPageState = compositionLocalOf<BrowserPagerState> {
  noLocalProvidedFor("LocalBrowserPageState")
}

class BrowserViewModel(
  private val browserController: BrowserController,
  private val browserNMM: NativeMicroModule,
  private val browserServer: HttpDwebServer,
) {
  private var webviewIdAcc: AtomicInt = atomic(1)
  private val currentBrowserBaseView: MutableState<BrowserWebView?> = mutableStateOf(null)
  private val browserViewList: MutableList<BrowserWebView> = mutableStateListOf() // 多浏览器列表
  val currentTab get() = currentBrowserBaseView.value
  val listSize get() = browserViewList.size
  val dwebLinkSearch: MutableState<String> = mutableStateOf("") // 为了获取desk传过来的地址信息
  val showSearchEngine: MutableTransitionState<Boolean> = MutableTransitionState(false)
  val showMultiView: MutableTransitionState<Boolean> = MutableTransitionState(false)
  val isNoTrace by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    runBlockingCatching {
      mutableStateOf(browserController.getStringFromStore(KEY_NO_TRACE)?.isNotEmpty() ?: false)
    }.getOrThrow()
  }

  fun getBookLinks() = browserController.bookLinks
  fun getHistoryLinks() = browserController.historyLinks

  // zf加的
  fun listFilter() = browserViewList.filter {
    it.viewItem.webView.getUrl().let {
      runCatching { if (it.startsWith("https://web.browser.dweb")) null else Url(it) }
    }.getOrNull() != null
  }

  fun getBrowserViewOrNull(currentPage: Int) =
    if (currentPage in 0..listSize) browserViewList[currentPage] else null

  private fun getBrowserMainUrl() = browserServer.startResult.urlInfo.buildInternalUrl().build {
    resolvePath("/index.html")
    parameters["api-base"] = browserServer.startResult.urlInfo.buildPublicUrl().toString()
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  fun rememberBrowserPagerState(): BrowserPagerState {
    debugBrowser("BrowserModel", "rememberBrowserPagerState -> $listSize")
    val pagerStateContent = rememberPagerState { listSize }
    val pagerStateNavigator = rememberPagerState { listSize }
    return BrowserPagerState(pagerStateContent, pagerStateNavigator)
  }

  /*  init {
      browserController.onCloseWindow {
        withMainContext {
          browserViewList.forEach { webview ->
            webview.viewItem.webView.destroy()
          }
          browserViewList.clear()
        }
      }
    }*/

  val searchBackBrowserView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    CoroutineScope(ioAsyncExceptionHandler).async {
      createBrowserWebView(createDwebView())
    }
  }

  private suspend fun createDwebView(url: String? = null) = IDWebView.create(
    browserNMM, DWebViewOptions(
      url = url ?: getBrowserMainUrl().toString(),
      /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
      detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
    )
  ) // .also { it.asAndroidWebView().isVerticalScrollBarEnabled = false } // 未解决

  private suspend fun getNewTabBrowserView(url: String? = null) =
    createBrowserWebView(createDwebView(url))

  private suspend fun createBrowserWebView(dWebView: IDWebView): BrowserWebView = withMainContext {
    val webviewId = "#w${webviewIdAcc.getAndAdd(1)}"
    val coroutineScope = CoroutineScope(CoroutineName(webviewId))
    val viewItem = DWebViewItem(
      webviewId = webviewId,
      webView = dWebView,
      coroutineScope = coroutineScope,
    )
    dWebView.onCreateWindow {
      val browserWebView = createBrowserWebView(it)
      if (browserViewList.add(browserWebView)) {
        focusBrowserView(browserWebView)
      }
    }
    BrowserWebView(viewItem)
  }

  private suspend fun focusBrowserView(view: BrowserWebView) {
    val index = browserViewList.indexOf(view)
    currentBrowserBaseView.value = view
    debugBrowser("focusBrowserView", "index=$index, size=${listSize}")
    updateMultiViewState(false, index)
  }

  internal suspend fun createNewTab(search: String? = null, url: String? = null) {
    // 先判断search是否不为空，然后在判断search是否是地址，
    dwebLinkSearch.value = "" // 先清空搜索的内容
    if (search?.isUrlOrHost() == true || url?.isUrlOrHost() == true) {
      addNewMainView(search ?: url)
    } else {
      val loadUrl = if (search?.isDeepLink() == false) {
        dwebLinkSearch.value = search
        search
      } else if (url?.isDeepLink() == false) {
        dwebLinkSearch.value = url
        url
      } else {
        getBrowserMainUrl().toString()
      }
      if (browserViewList.isEmpty()) {
        addNewMainView(loadUrl)
      } else {
        searchWebView(loadUrl)
      }
    }
  }

  fun openDwebBrowser(mmid: MMID) = browserController.openDwebBrowser(mmid)

  suspend fun removeBrowserWebView(browserWebView: BrowserWebView) {
    browserViewList.remove(browserWebView) // 需要释放掉
    withMainContext {
      browserWebView.viewItem.webView.destroy()
      // 如果移除后，发现列表空了，手动补充一个
      if (browserViewList.isEmpty()) {
        getNewTabBrowserView().also {
          browserViewList.add(it)
          currentBrowserBaseView.value = it
          updateMultiViewState(false, 0)
        }
      }
    }
  }

  fun updateCurrentBrowserView(currentPage: Int) {
    if (currentPage in 0..<listSize) {
      currentBrowserBaseView.value = browserViewList[currentPage]
    }
  }

  private val pagerChangeSignal: Signal<Int> = Signal()
  val onPagerStateChange = pagerChangeSignal.toListener()
  suspend fun updateMultiViewState(show: Boolean, index: Int? = null) {
    showMultiView.targetState = show
    delay(100) // window没渲染，导致scroll操作没效果，所以这边增加点等待
    index?.let { pagerChangeSignal.emit(index) }
  }

  suspend fun searchWebView(url: String) = withContext(ioAsyncExceptionHandler) {
    showSearchEngine.targetState = false // 到搜索功能了，搜索引擎必须关闭
    val loadingState = currentBrowserBaseView.value?.loadState
    loadingState?.value = true
    if (url.isDeepLink()) { // 负责拦截browser的dweb_deeplink
      browserNMM.nativeFetch(url)
      loadingState?.value = false
    } else {
      currentBrowserBaseView.value?.viewItem?.apply {
        webView.loadUrl(url, true/* 强制加载 */)
      }
      loadingState?.value = false
    }
  }

  suspend fun addNewMainView(url: String? = null) = withMainContext {
    val itemView = getNewTabBrowserView(url)
    browserViewList.add(itemView)
    focusBrowserView(itemView)
  }

  suspend fun openQRCodeScanning() {
    val data = browserNMM.nativeFetch("file://barcode-scanning.sys.dweb/open").body.toPureString()
    // 如果是url，进行跳转，如果不是，就直接弹出对话框
    if (data.isNotEmpty()) {
      searchWebView(data)
    }
  }

  suspend fun shareWebSiteInfo() {
    currentBrowserBaseView.value?.let {
      val url = it.viewItem.webView.getUrl()
      if (url.isSystemUrl()) {
        // handleIntent(BrowserIntent.ShowSnackbarMessage("无效的分享"))
        return@let
      }
      val title = it.viewItem.webView.getTitle()

      val request =
        PureRequest("file://share.sys.dweb/share?title=${title}&url=$url", IpcMethod.POST)
      browserNMM.nativeFetch(request)
    }
  }

  /**
   * 添加到桌面功能
   */
  suspend fun addUrlToDesktop(): Boolean {
    return currentBrowserBaseView.value?.let { browserWebView ->
      val webView = browserWebView.viewItem.webView
      val url = webView.getUrl()
      browserController.addUrlToDesktop(
        title = webView.getTitle().ifEmpty { url }, url = url, icon = webView.getIcon()
      )
    } ?: false
  }

  suspend fun saveBrowserMode(noTrace: Boolean) {
    isNoTrace.value = noTrace
    browserController.saveStringToStore(KEY_NO_TRACE, if (noTrace) "true" else "")
  }

  /**
   * 存储最后的搜索内容
   */
  suspend fun saveLastKeyword(inputText: MutableState<String>, url: String) {
    browserController.saveStringToStore(KEY_LAST_SEARCH_KEY, url)
    inputText.value = url
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
        removeAll { it.url == addUrl } // 删除同一天的重复数据
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
}

/**
 * 根据内容解析成需要显示的内容
 */
internal fun parseInputText(text: String, needHost: Boolean = true): String {
  val url = Url(text)
  for (item in DefaultAllWebEngine) {
    if (item.fit(text)) return url.parameters[item.queryName()]!!
  }
  if (text.startsWith("dweb:") || text.startsWith("about:")) {
    return text
  }
  return if (needHost && url.host.isNotEmpty()) {
    url.host
  } else if (url.parameters["text"]?.isNotEmpty() == true) {
    url.parameters["text"]!!
  } else {
    text
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
        icon = null //getIconBitmap() // 这也有一个
      )
    } else null
  }
}