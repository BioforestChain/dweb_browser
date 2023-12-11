package org.dweb_browser.browser.web.ui.model

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.browser.LocalBitmapManager
import org.dweb_browser.browser.util.isDeepLink
import org.dweb_browser.browser.util.isSystemUrl
import org.dweb_browser.browser.util.isUrlOrHost
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.browser.web.model.BrowserWebView
import org.dweb_browser.browser.web.model.ConstUrl
import org.dweb_browser.browser.web.model.KEY_LAST_SEARCH_KEY
import org.dweb_browser.browser.web.model.KEY_NO_TRACE
import org.dweb_browser.browser.web.model.WebSiteInfo
import org.dweb_browser.browser.web.model.WebSiteType
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.base.DWebViewItem
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.build
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.noLocalProvidedFor
import org.dweb_browser.helper.platform.toByteArray
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.withMainContext
/**
 * 用于显示搜索的界面，也就是点击搜索框后界面
 */
val LocalShowSearchView = compositionLocalOf {
  mutableStateOf(false)
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

@OptIn(ExperimentalFoundationApi::class)
class BrowserViewModel(
  private val browserController: BrowserController,
  private val browserNMM: NativeMicroModule,
  private val browserServer: HttpDwebServer,
) {
  private var webviewIdAcc by SafeInt(1)
  private val currentBrowserBaseView: MutableState<BrowserWebView?> = mutableStateOf(null)
  private val browserViewList: MutableList<BrowserWebView> = mutableStateListOf() // 多浏览器列表
  val currentTab get() = currentBrowserBaseView.value
  val listSize get() = browserViewList.size
  val dwebLinkSearch: MutableState<String> = mutableStateOf("") // 为了获取desk传过来的地址信息
  val showSearchEngine: MutableTransitionState<Boolean> = MutableTransitionState(false)
  val showMultiView: MutableTransitionState<Boolean> = MutableTransitionState(false)
  val isNoTrace by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    mutableStateOf(browserController.isNoTrace)
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

  private suspend fun focusBrowserView(view: BrowserWebView) {
    val index = browserViewList.indexOf(view)
    focusBrowserView(index)
  }

  private val pagerChangeSignal: Signal<Int> = Signal()
  private val onPagerChange = pagerChangeSignal.toListener()

  private suspend fun focusBrowserView(index: Int) = browserController.ioAsyncScope.launch {
    if (index in 0 until listSize) {
      currentBrowserBaseView.value = browserViewList[index]
      showMultiView.targetState = false
      showSearchEngine.targetState = false
      debugBrowser("focusBrowserView", "index=$index, size=${listSize}")
      pagerChangeSignal.emit(index)
    }
  }

  private var browserPagerState: BrowserPagerState? = null

  @Composable
  fun rememberBrowserPagerState(): BrowserPagerState {
    val pagerStateContent = rememberPagerState { listSize }
    val pagerStateNavigator = rememberPagerState { listSize }
    return BrowserPagerState(pagerStateContent, pagerStateNavigator).also {
      browserPagerState = it
    }
  }

  init {
    browserController.onCloseWindow {
      withMainContext {
        browserViewList.forEach { webview ->
          webview.viewItem.webView.destroy()
        }
        browserViewList.clear()
      }
    }
    onPagerChange { pagerIndex ->
      delay(100) // 避免执行的时候界面还未加载导致滑动没起作用
      withMainContext {
        browserPagerState?.pagerStateContent?.scrollToPage(pagerIndex)
        browserPagerState?.pagerStateNavigator?.scrollToPage(pagerIndex)
      }
    }
  }

  suspend fun updateMultiViewState(show: Boolean, index: Int? = null) {
    showMultiView.targetState = show
    if (!show) {
      index?.let { focusBrowserView(index) }
    }
  }

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
  ).also { it.setVerticalScrollBarVisible(false) }

  private suspend fun getNewTabBrowserView(url: String? = null) =
    createBrowserWebView(createDwebView(url))

  private suspend fun createBrowserWebView(dWebView: IDWebView): BrowserWebView = withMainContext {
    val webviewId = "#w${webviewIdAcc++}"
    val coroutineScope = CoroutineScope(CoroutineName(webviewId))
    val viewItem = DWebViewItem(
      webviewId = webviewId,
      webView = dWebView,
      coroutineScope = coroutineScope,
    ).also { it.webView.setVerticalScrollBarVisible(false) }
    dWebView.onCreateWindow {
      val browserWebView = createBrowserWebView(it)
      if (browserViewList.add(browserWebView)) {
        focusBrowserView(browserWebView)
      }
    }
    BrowserWebView(viewItem)
  }

  internal suspend fun openBrowserView(search: String? = null, url: String? = null) {
    // 先判断search是否不为空，然后在判断search是否是地址，
    debugBrowser("openBrowserView", "search=$search, url=$url")
    dwebLinkSearch.value = search ?: url ?: ConstUrl.BLANK.url
  }

  /**
   * 为了适应 ios，从而将 webview 的处理独立开
   */
  private suspend fun parseDwebLinkSearch(url: String): Boolean {
    if (url == ConstUrl.BLANK.url && browserViewList.isNotEmpty()) return false
    return if ((url.isEmpty() || url == ConstUrl.BLANK.url) && browserViewList.isEmpty()) {
      addNewMainView(getBrowserMainUrl().toString())
      false
    } else if (url.isUrlOrHost()) {
      addNewMainView(url)
      false
    } else {
      url.isNotEmpty()
    }
  }

  @Composable
  fun BrowserSearchConfig() {
    // 增加判断是否有传入需要检索的内容，如果有，就进行显示搜索界面
    val showSearchView = LocalShowSearchView.current
    LaunchedEffect(dwebLinkSearch) {
      snapshotFlow { dwebLinkSearch.value }.collect { searchUrl ->
        if (parseDwebLinkSearch(searchUrl)) {
          showSearchView.value = true
        }
      }
    }
    LaunchedEffect(showSearchView) {
      snapshotFlow { showSearchView.value }.collect { show ->
        if (!show) {
          dwebLinkSearch.value = ""
        }
      }
    }
  }

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
    currentBrowserBaseView.value
  }

  suspend fun addNewMainView(url: String? = null) = withMainContext {
    getNewTabBrowserView(url).also { itemView ->
      browserViewList.add(itemView)
      focusBrowserView(itemView)
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

  suspend fun createDesktopLink(link: String, title: String, iconString: String){
    browserController.addUrlToDesktop(
      title = title, url = link, icon = iconString
    )
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
  fun changeBookLink(add: WebSiteInfo? = null, del: WebSiteInfo? = null) {
    println("MMIke changeBookLink: add: ${add?.title} del:${del?.title}")
    browserController.ioAsyncScope.launch {
      add?.apply {
        browserController.bookLinks.add(this)
//        this.iconImage?.let { icon -> LocalBitmapManager.saveImageBitmap(this.id, icon) }
        browserController.saveBookLinks()
      }
      del?.apply {
        browserController.bookLinks.remove(this)
        browserController.saveBookLinks()
//        LocalBitmapManager.deleteImageBitmap(this.id)
      }
    }
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

  suspend fun loadMoreHistory(off: Int) {
    browserController.loadMoreHistory(off)
  }
}

/**
 * 根据内容解析成需要显示的内容
 */
internal fun parseInputText(text: String, needHost: Boolean = true): String {
  for (item in DefaultAllWebEngine) {
    if (item.fit(text)) return Url(text).parameters[item.queryName()]!!
  }
  if (text.startsWith("dweb:") || text.startsWith("about:") ||
    (text.isUrlOrHost() && !text.startsWith("http") /*表示域名*/)) {
    return text
  }
  val url = Url(text)
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
        icon = getFavoriteIcon()?.toByteArray() // 这也有一个
      )
    } else null
  }
}