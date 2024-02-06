package org.dweb_browser.browser.web.model

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.util.isDeepLink
import org.dweb_browser.browser.util.isSystemUrl
import org.dweb_browser.browser.util.isUrlOrHost
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.data.BrowserContentItem
import org.dweb_browser.browser.web.data.ConstUrl
import org.dweb_browser.browser.web.data.KEY_LAST_SEARCH_KEY
import org.dweb_browser.browser.web.data.KEY_NO_TRACE
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.data.WebSiteType
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.base.DWebViewItem
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.toByteArray
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.share.ext.postSystemShare
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermission
import org.dweb_browser.sys.toast.ext.showToast

val LocalBrowserModel = compositionChainOf<BrowserViewModel>("BrowserModel")

/**
 * 用于显示搜索的界面，也就是点击搜索框后界面
 */
val LocalShowSearchView = compositionChainOf("ShowSearchView") {
  mutableStateOf(false)
}

val LocalShowIme = compositionChainOf("ShowIme") {
  mutableStateOf(false)
}

/**
 * 用于指定输入的内容
 */
val LocalInputText = compositionChainOf("InputText") {
  mutableStateOf("")
}

data class BrowserPagerState @OptIn(ExperimentalFoundationApi::class) constructor(
  val pagerStateContent: PagerState, // 用于表示展示内容
  val pagerStateNavigator: PagerState, // 用于表示下面搜索框等内容
)

data class DwebLinkSearchItem(val link: String, val blank: Boolean) {
  companion object {
    val Empty = DwebLinkSearchItem("", false)
  }
}

val LocalBrowserPageState = compositionChainOf<BrowserPagerState>("LocalBrowserPageState")

@OptIn(ExperimentalFoundationApi::class)
class BrowserViewModel(
  private val browserController: BrowserController,
  val browserNMM: NativeMicroModule,
  private val browserServer: HttpDwebServer,
) {
  private var webviewIdAcc by SafeInt(1)
  private val currentBrowserContentItem: MutableState<BrowserContentItem?> = mutableStateOf(null)
  private val browserContentItems: MutableList<BrowserContentItem> = mutableStateListOf() // 多浏览器列表
  val currentTab get() = currentBrowserContentItem.value
  val listSize get() = browserContentItems.size
  val dwebLinkSearch: MutableState<DwebLinkSearchItem> =
    mutableStateOf(DwebLinkSearchItem.Empty) // 为了获取desk传过来的地址信息
  val showSearchEngine: MutableTransitionState<Boolean> = MutableTransitionState(false)
  val showMultiView: MutableTransitionState<Boolean> = MutableTransitionState(false)
  val isNoTrace by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    mutableStateOf(browserController.isNoTrace)
  }

  val filterShowEngines
    get() = browserController.searchEngines.filter { webEngine ->
      webEngine.checked
    }

  fun filterFitUrlEngines(url: String) = browserController.searchEngines.firstOrNull { it.fit(url) }
  fun getSearchEngines() = browserController.searchEngines
  fun getBookLinks() = browserController.bookLinks
  fun getHistoryLinks() = browserController.historyLinks
  val browserOnVisible = browserController.onWindowVisiable
  val browserOnClose = browserController.onCloseWindow

  fun getBrowserViewOrNull(currentPage: Int) =
    if (currentPage in 0..listSize) browserContentItems[currentPage] else null

  private suspend fun focusBrowserView(view: BrowserContentItem) {
    val index = browserContentItems.indexOf(view)
    focusBrowserView(index)
  }

  private val pagerChangeSignal: Signal<Int> = Signal()
  private val onPagerChange = pagerChangeSignal.toListener()

  private suspend fun focusBrowserView(index: Int) = browserController.ioAsyncScope.launch {
    if (index in 0 until listSize) {
      currentBrowserContentItem.value = browserContentItems[index]
      showMultiView.targetState = false
      showSearchEngine.targetState = false
      debugBrowser("focusBrowserView", "index=$index, size=${listSize}")
      pagerChangeSignal.emit(index)
    }
  }

  /**
   * 请求系统权限
   */
  suspend fun requestSystemPermission(
    title: String = "",
    description: String = "",
    permissionName: SystemPermissionName
  ): Boolean {
    return browserNMM.requestSystemPermission(
      SystemPermissionTask(
        permissionName,
        title = title,
        description = description
      )
    )
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
      browserContentItems.forEach { browserContentItem -> closeWebView(browserContentItem) }
      browserContentItems.clear()
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

  private suspend fun createDwebView(url: String) = IDWebView.create(
    browserNMM, DWebViewOptions(
      url = url,
      /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
      detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
    )
  ).also { it.setVerticalScrollBarVisible(false) }

  private suspend fun createContentWebView(dWebView: IDWebView): BrowserContentItem.ContentWebItem =
    withMainContext {
      val webviewId = "#w${webviewIdAcc++}"
      val coroutineScope = CoroutineScope(CoroutineName(webviewId))
      val viewItem = DWebViewItem(
        webviewId = webviewId,
        webView = dWebView,
        coroutineScope = coroutineScope,
      ).also { it.webView.setVerticalScrollBarVisible(false) }
      dWebView.onCreateWindow {
        val browserContentItem = createBrowserContentItem()
        if (browserContentItems.add(browserContentItem)) {
          focusBrowserView(browserContentItem)
        }
      }
      BrowserContentItem.ContentWebItem(viewItem)
    }

  private suspend fun createContentWebView(url: String) = createContentWebView(createDwebView(url))

  private suspend fun createBrowserContentItem(url: String? = null) =
    BrowserContentItem().apply { contentWebItem.value = url?.let { createContentWebView(url) } }

  internal suspend fun openBrowserView(
    search: String? = null,
    url: String? = null,
    blank: Boolean? = false
  ) {
    // 先判断search是否不为空，然后在判断search是否是地址，
    debugBrowser("openBrowserView", "search=$search, url=$url")
    dwebLinkSearch.value = DwebLinkSearchItem(search ?: url ?: ConstUrl.BLANK.url, blank ?: false)
  }

  /**
   * 为了适应 ios，从而将 webview 的处理独立开
   */
  private suspend fun parseDwebLinkSearch(url: String): Boolean {
    if (url == ConstUrl.BLANK.url && browserContentItems.isNotEmpty()) return false
    return if ((url.isEmpty() || url == ConstUrl.BLANK.url) && browserContentItems.isEmpty()) {
      addNewMainView()
      false
    } else if (url.isUrlOrHost()) {
      // 判断如果已存在，直接focus，不新增界面
      browserContentItems.find { browserContentItem ->
        browserContentItem.contentWebItem.value?.let { contentWebItem ->
          contentWebItem.viewItem.webView.getUrl() == url
        } ?: false
      }?.let { browserWebView ->
        focusBrowserView(browserWebView)
      } ?: run {
        addNewMainView(url)
      }
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
      snapshotFlow { dwebLinkSearch.value }.collect { searchItem ->
        if (parseDwebLinkSearch(searchItem.link)) {
          showSearchView.value = true
        }
      }
    }
    LaunchedEffect(showSearchView) {
      snapshotFlow { showSearchView.value }.collect { show ->
        if (!show) {
          dwebLinkSearch.value = DwebLinkSearchItem.Empty
        }
      }
    }
  }

  suspend fun closeWebView(browserContentItem: BrowserContentItem) {
    browserContentItem.contentWebItem.value?.let { contentWebItem ->
      withMainContext {
        contentWebItem.viewItem.webView.destroy()
        browserContentItem.contentWebItem.value = null
      }
    }
  }

  suspend fun closeBrowserContentItem(browserContentItem: BrowserContentItem) {
    browserContentItems.remove(browserContentItem) // 需要释放掉
    closeWebView(browserContentItem)
    // 如果移除后，发现列表空了，手动补充一个
    if (browserContentItems.isEmpty()) {
      withMainContext {
        BrowserContentItem().also {
          browserContentItems.add(it)
          currentBrowserContentItem.value = it
          updateMultiViewState(false, 0)
        }
      }
    }
  }

  fun updateCurrentBrowserView(currentPage: Int) {
    if (currentPage in 0..<listSize) {
      currentBrowserContentItem.value = browserContentItems[currentPage]
    }
  }

  /**
   * 滑动搜索栏时，需要做一次截屏
   */
  suspend fun captureBrowserWebView(currentPage: Int) {
    browserContentItems.getOrNull(currentPage)?.captureView()
  }

  suspend fun searchWebView(url: String) = withContext(ioAsyncExceptionHandler) {
    showSearchEngine.targetState = false // 到搜索功能了，搜索引擎必须关闭
    currentTab?.let { browserContentItem ->
      if (url.isDeepLink()) { // 负责拦截browser的dweb_deeplink
        browserNMM.nativeFetch(url)
      } else {
        if (browserContentItem.contentWebItem.value == null) {
          browserContentItem.contentWebItem.value = createContentWebView(url)
        }
        browserContentItem.contentWebItem.value?.apply {
          loadState.value = true
          viewItem.webView.loadUrl(url, true/* 强制加载 */)
          loadState.value = false
        }
      }
    }
  }

  suspend fun addNewMainView(url: String? = null) = withMainContext {
    createBrowserContentItem(url).also { browserContentItem ->
      browserContentItems.add(browserContentItem)
      focusBrowserView(browserContentItem)
    }
  }

  suspend fun shareWebSiteInfo() {
    currentTab?.contentWebItem?.value?.let { contentWebItem ->
      val url = contentWebItem.viewItem.webView.getUrl()
      if (url.isSystemUrl()) {
        showToastMessage(BrowserI18nResource.toast_message_add_book_invalid.text)
        return@let
      }
      val title = contentWebItem.viewItem.webView.getTitle()
      browserNMM.postSystemShare(title = title, url = url).let { result ->
        if (!result.success) {
          showToastMessage(result.message)
        }
      }
    } ?: showToastMessage(BrowserI18nResource.toast_message_add_book_invalid.text)
  }

  /**
   * 添加到桌面功能
   */
  suspend fun addUrlToDesktop(): Boolean {
    return currentTab?.contentWebItem?.value?.let { contentWebItem ->
      val webView = contentWebItem.viewItem.webView
      val url = webView.getOriginalUrl()
      addUrlToDesktop(
        title = webView.getTitle().ifEmpty { url }, link = url, iconString = webView.getIcon()
      )
    } ?: false
  }

  suspend fun addUrlToDesktop(title: String, link: String, iconString: String): Boolean {
    return browserController.addUrlToDesktop(
      title = title, url = link, icon = iconString
    ).also { result ->
      showToastMessage(
        if (result) {
          BrowserI18nResource.toast_message_add_desk_success.text
        } else {
          BrowserI18nResource.toast_message_add_desk_exist.text
        }
      )
    }
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
  fun addBookLink(item: WebSiteInfo) = browserController.ioAsyncScope.launch {
    showToastMessage(BrowserI18nResource.toast_message_add_book.text)
    browserController.bookLinks.add(item)
    browserController.saveBookLinks()
  }

  fun removeBookLink(item: WebSiteInfo) = browserController.ioAsyncScope.launch {
    showToastMessage(BrowserI18nResource.toast_message_remove_book.text)
    browserController.bookLinks.remove(item)
    browserController.saveBookLinks()
  }

  fun updateBookLink(item: WebSiteInfo) = browserController.ioAsyncScope.launch {
    showToastMessage(BrowserI18nResource.toast_message_update_book.text)
    browserController.saveBookLinks()
  }

  /**
   * 操作历史数据
   * 新增：需要新增数据
   * 修改：历史数据没有修改
   * 删除：需要删除数据
   */
  suspend fun addHistoryLink(item: WebSiteInfo) {
    if (isNoTrace.value) return // 如果是无痕模式，则不能进行存储历史操作
    val key = item.timeMillis.toString()
    val addUrl = item.url
    browserController.historyLinks.getOrPut(key) {
      mutableStateListOf()
    }.apply {
      removeAll { it.url == addUrl } // 删除同一天的重复数据
      add(0, item)
      browserController.saveHistoryLinks(key, this)
    }
  }

  suspend fun removeHistoryLink(item: WebSiteInfo) {
    val key = item.timeMillis.toString()
    browserController.historyLinks[key]?.apply {
      showToastMessage(BrowserI18nResource.toast_message_remove_history.text)
      remove(item)
      browserController.saveHistoryLinks(key, this)
    }
  }

  /**
   * 搜索引擎
   */
  fun addSearchEngine(item: WebEngine) = browserController.ioAsyncScope.launch {
    browserController.searchEngines.add(item)
    browserController.saveSearchEngines()
  }

  fun removeSearchEngine(item: WebEngine) = browserController.ioAsyncScope.launch {
    browserController.searchEngines.remove(item)
    browserController.saveSearchEngines()
  }

  fun updateSearchEngine(item: WebEngine) = browserController.ioAsyncScope.launch {
    browserController.saveSearchEngines()
  }

  suspend fun loadMoreHistory(off: Int) {
    browserController.loadMoreHistory(off)
  }

  fun showToastMessage(message: String) {
    browserController.ioAsyncScope.launch { browserNMM.showToast(message) }
  }
}

/**
 * 根据内容解析成需要显示的内容
 */
internal fun parseInputText(text: String, needHost: Boolean = true): String {
  val url = Url(text)
  for (item in DefaultAllWebEngine) {
    if (item.host == url.host) return url.parameters[item.queryName()] ?: url.host
  }
  if (text.startsWith("dweb:") || text.startsWith("about:") ||
    (text.isUrlOrHost() && !text.startsWith("http") /*表示域名*/)
  ) {
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
        icon = getFavoriteIcon()?.toByteArray() // 这也有一个
      )
    } else null
  }
}