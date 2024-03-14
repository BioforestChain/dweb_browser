package org.dweb_browser.browser.web.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.search.SearchEngine
import org.dweb_browser.browser.search.SearchInject
import org.dweb_browser.browser.search.ext.createChannelOfEngines
import org.dweb_browser.browser.search.ext.isEngineAndGetHomeLink
import org.dweb_browser.browser.util.isDeepLink
import org.dweb_browser.browser.util.isOnlyHost
import org.dweb_browser.browser.util.isSystemUrl
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.data.page.BrowserBookmarkPage
import org.dweb_browser.browser.web.data.page.BrowserDownloadPage
import org.dweb_browser.browser.web.data.page.BrowserHistoryPage
import org.dweb_browser.browser.web.data.page.BrowserHomePage
import org.dweb_browser.browser.web.data.page.BrowserPage
import org.dweb_browser.browser.web.data.page.BrowserSettingPage
import org.dweb_browser.browser.web.data.page.BrowserWebPage
import org.dweb_browser.browser.web.data.ConstUrl
import org.dweb_browser.browser.web.data.KEY_LAST_SEARCH_KEY
import org.dweb_browser.browser.web.data.KEY_NO_TRACE
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.data.WebSiteType
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.platform.toByteArray
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermission
import org.dweb_browser.sys.share.ext.postSystemShare
import org.dweb_browser.sys.toast.ext.showToast

val LocalBrowserViewModel = compositionChainOf<BrowserViewModel>("BrowserModel")

@Composable
fun rememberBrowserViewModal() = LocalBrowserViewModel.current

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

data class DwebLinkSearchItem(val link: String, val target: String) {
  companion object {
    val Empty = DwebLinkSearchItem("", "_self")
  }
}

val LocalBrowserPageState = compositionChainOf<BrowserPagerState>("LocalBrowserPageState")

@OptIn(ExperimentalFoundationApi::class)
class BrowserViewModel(
  private val browserController: BrowserController, internal val browserNMM: NativeMicroModule,
) {
  private val pages: MutableList<BrowserPage> = mutableStateListOf() // 多浏览器列表
  var showMore by mutableStateOf(false)
  var showPreview by mutableStateOf(false)
  var scale by mutableStateOf(1f)

  var focusPage by mutableStateOf<BrowserPage?>(null)
    private set
  val listSize get() = pages.size
  val dwebLinkSearch = mutableStateOf(DwebLinkSearchItem.Empty) // 为了获取desk传过来的地址信息
  var showSearchEngine by mutableStateOf(false)
  val isNoTrace by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    mutableStateOf(browserController.isNoTrace)
  }
  val showDownloadManage = mutableStateOf(false)

  val searchInjectList = mutableStateListOf<SearchInject>()
  val searchEngineList = mutableStateListOf<SearchEngine>()
  val filterShowEngines get() = searchEngineList.filter { it.enable }
  fun filterFitUrlEngines(url: String) = searchEngineList.firstOrNull { it.fit(url) }

  fun checkAndSearch(key: String, hide: () -> Unit) = browserNMM.ioAsyncScope.launch {
    val homeLink = browserNMM.isEngineAndGetHomeLink(key) // 将关键字对应的搜索引擎置为有效
    debugBrowser("checkAndSearch", "homeLink=$homeLink")
    if (homeLink.isNotEmpty()) { // 使用首页地址直接打开网页
      hide()
      doSearch(homeLink)
    }
  }

  fun getBookmarks() = browserController.bookmarks.value
  fun getHistoryLinks() = browserController.historys.value
  val browserOnVisible = browserController.onWindowVisible
  val browserOnClose = browserController.onCloseWindow

  fun getBrowserViewOrNull(currentPage: Int) = pages.getOrNull(currentPage)


  private val pagerChangeSignal: Signal<Int> = Signal()
  private val onPagerChange = pagerChangeSignal.toListener()
  private suspend fun focusPage(view: BrowserPage) {
    val index = pages.indexOf(view)
    focusPage(index)
  }

  private suspend fun focusPage(index: Int) {
    pages.getOrNull(index)?.also {
      focusPage?.captureViewInBackground()
      focusPage = it
      showPreview = false
      debugBrowser("focusBrowserView", "index=$index, size=${listSize}")
      pagerChangeSignal.emit(index)
    }
  }

  /**
   * 请求系统权限
   */
  suspend fun requestSystemPermission(
    title: String = "", description: String = "", permissionName: SystemPermissionName
  ): Boolean {
    return browserNMM.requestSystemPermission(
      SystemPermissionTask(
        permissionName, title = title, description = description
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
      pages.forEach { browserContentItem -> closePage(browserContentItem) }
      pages.clear()
    }
    onPagerChange { pagerIndex ->
      delay(100) // 避免执行的时候界面还未加载导致滑动没起作用
      withMainContext {
        browserPagerState?.pagerStateContent?.scrollToPage(pagerIndex)
        browserPagerState?.pagerStateNavigator?.scrollToPage(pagerIndex)
      }
    }

    browserNMM.ioAsyncScope.launch {
      browserNMM.createChannelOfEngines {
        searchEngineList.clear()
        searchEngineList.addAll(engineList)
      }
    }
  }

  suspend fun updatePreviewState(show: Boolean, index: Int? = null) {
    showPreview = show
    if (!show) {
      index?.let { focusPage(index) }
    }
  }

  private suspend fun createDwebView(url: String) = IDWebView.create(
    browserNMM, DWebViewOptions(
      url = url,
      /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
      detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
    )
  ).also { it.setVerticalScrollBarVisible(false) }

  private suspend fun createWebPage(dWebView: IDWebView) = withMainContext {
    dWebView.onCreateWindow { itemDwebView ->
      val newWebPage = BrowserWebPage(itemDwebView, browserController)
      addNewPage(newWebPage)
    }
    dWebView.onDownloadListener { args: WebDownloadArgs ->
      debugBrowser("download", args)
      browserController.openDownloadView(args)
    }
    BrowserWebPage(dWebView, browserController)
  }

  private suspend fun createWebPage(url: String) = createWebPage(createDwebView(url))


  internal suspend fun openBrowserView(
    search: String? = null, url: String? = null, target: String? = null
  ) {
    // 先判断search是否不为空，然后在判断search是否是地址，
    debugBrowser("openBrowserView", "search=$search, url=$url, target=$target")
    dwebLinkSearch.value =
      DwebLinkSearchItem(search ?: url ?: ConstUrl.BLANK.url, target ?: "_self")
  }

  /**
   * 如果找到同样url的页面，那么聚焦那个页面
   * 如果没有找到，同时它又是合法的url，那么创建新页面打开
   * 否则，走 unknownUrl 回调
   *
   * > 为了适应 ios，从而将 webview 的处理独立开
   */
  suspend fun tryOpenUrl(
    url: String, unknownUrl: (suspend (String) -> Unit)? = null
  ) {
    // 判断如果已存在，直接focus，不新增界面
    when (val samePage = pages.find { page ->
      page.isUrlMatch(url)
    }) {
      null -> {
        // 尝试添加新页面
        val newPage = addNewPage(url)
        // 否则走未知模式
        if (newPage == null) {
          url.isNotEmpty().trueAlso { unknownUrl?.invoke(url) }
        }
      }

      else -> {
        focusPage(samePage)
        samePage.updateUrl(url)
      }
    }
  }

  @Composable
  fun BrowserSearchConfig() {
    // 增加判断是否有传入需要检索的内容，如果有，就进行显示搜索界面
    val showSearchView = LocalShowSearchView.current
    LaunchedEffect(dwebLinkSearch) {
      snapshotFlow { dwebLinkSearch.value }.collect { searchItem ->
        tryOpenUrl(searchItem.link) {
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

  suspend fun closePage(page: BrowserPage): Boolean {
    val index = pages.indexOf(page)
    if (index == -1) {
      return false
    }
    if (focusPage == page) {
      val newFocusIndex = maxOf(index + 1, pages.size - 1)
      if (newFocusIndex != index) {
        focusPage(newFocusIndex)
      }
    }
    pages.removeAt(index)
    page.destroy()
    // 如果移除后，发现列表空了，手动补充一个
    if (pages.isEmpty()) {
      addNewPage(BrowserHomePage(browserController), true)
    }
    return true
  }

  fun updateCurrentBrowserView(currentPage: Int) {
    pages.getOrNull(currentPage)?.also {
      this.focusPage = it
    }
  }

  /**
   * 滑动搜索栏时，需要做一次截屏
   */
  suspend fun capturePage(pageIndex: Int) {
    debugBrowser("capturePage") { "pageIndex=$pageIndex" }
    pages.getOrNull(pageIndex)?.captureView()
  }

  fun doSearch(url: String) = browserNMM.ioAsyncScope.launch {
    // 到搜索功能了，搜索引擎必须关闭
    showSearchEngine = false
    // 存储最后的搜索内容
    browserController.saveStringToStore(KEY_LAST_SEARCH_KEY, url)

    // 如果是 dweb_deeplink，直接代理访问。否则就是打开新页面
    if (url.isDeepLink()) browserNMM.nativeFetch(url) else addNewPage(url)
  }

  suspend fun addNewPage(url: String? = null, focus: Boolean = true): BrowserPage? {
    val newPage = if (url == null || BrowserHomePage.isNewTabUrl(url)) {
      if (focusPage is BrowserHomePage) {
        return focusPage
      }
      BrowserHomePage(browserController)
    } else if (BrowserWebPage.isWebUrl(url)) {
      createWebPage(url)
    } else if (BrowserBookmarkPage.isBookmarkUrl(url)) {
      BrowserBookmarkPage(browserController)
    } else if (BrowserHistoryPage.isHistoryUrl(url)) {
      BrowserHistoryPage(browserController)
    } else if (BrowserDownloadPage.isDownloadUrl(url)) {
      BrowserDownloadPage(browserController)
    } else if (BrowserSettingPage.isSettingUrl(url)) {
      BrowserSettingPage(browserController)
    } else null
    if (newPage != null) {
      val preFocusPage = focusPage
      addNewPage(newPage, focus)
      if (preFocusPage != focusPage && preFocusPage is BrowserHomePage) {
        closePage(preFocusPage)
      }
    }
    return newPage
  }

  suspend fun addNewPage(page: BrowserPage, focus: Boolean = true) {
    pages.add(page)
    focusPage(page)
  }

  suspend fun shareWebSiteInfo(page: BrowserWebPage) {
    browserNMM.postSystemShare(title = page.title, url = page.url).let { result ->
      if (!result.success) {
        showToastMessage(result.message)
      }
    }
  }

  /**
   * 添加到桌面功能
   */
  suspend fun addUrlToDesktop(): Boolean {
    return this.focusPage?.let { page ->
      if (page is BrowserWebPage) {
        val webView = page.webView
        val url = webView.getOriginalUrl()
        addUrlToDesktop(
          title = webView.getTitle().ifEmpty { url }, link = url, iconString = webView.getIcon()
        )
      } else false
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

  suspend fun addBookmark(webPage: BrowserWebPage) =
    webPage.webView.toWebSiteInfo(WebSiteType.Bookmark)?.also {
      addBookmark(it)
    } ?: showToastMessage(BrowserI18nResource.toast_message_add_book_invalid.text)

  suspend fun removeBookmark(url: String) = browserController.bookmarks.value.filter {
    it.url == url
  }.map { removeBookmark(it).join() }

  /**
   * 操作书签数据
   * 新增：需要新增数据
   * 修改：该对象已经变更，可直接保存，所以不需要传
   * 删除：需要删除数据
   */
  fun addBookmark(item: WebSiteInfo) = browserController.ioAsyncScope.launch {
    showToastMessage(BrowserI18nResource.toast_message_add_book.text)
    browserController.bookmarks.value += item
    browserController.saveBookLinks()
  }

  fun removeBookmark(item: WebSiteInfo) = browserController.ioAsyncScope.launch {
    showToastMessage(BrowserI18nResource.toast_message_remove_book.text)
    browserController.bookmarks.value -= item
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
    val dayKey = item.day.toString()
    val addUrl = item.url
    browserController.historys.update { historyMap ->
      val dayList = historyMap[dayKey]?.apply {
        toMutableList().apply {
          removeAll { item -> item.url == addUrl } // 删除同一天的重复数据
          add(0, item)
        }.toList()
      } ?: listOf(item)
      browserController.saveHistoryLinks(dayKey, dayList)

      historyMap + Pair(dayKey, dayList)
    }
  }

  suspend fun removeHistoryLink(item: WebSiteInfo) {
    val dayKey = item.day.toString()
    browserController.historys.update { historyMap ->
      val dayList = historyMap[dayKey]?.filter { it.url != item.url } ?: return@update historyMap
      browserController.saveHistoryLinks(dayKey, dayList)
      historyMap + Pair(dayKey, dayList)
    }
  }

  /**
   * 搜索引擎
   */
//  fun addSearchEngine(item: WebEngine) = browserController.ioAsyncScope.launch {
//    browserController.searchEngines.add(item)
//    browserController.saveSearchEngines()
//  }
//
//  fun removeSearchEngine(item: WebEngine) = browserController.ioAsyncScope.launch {
//    browserController.searchEngines.remove(item)
//    browserController.saveSearchEngines()
//  }
//
//  fun updateSearchEngine(item: WebEngine) = browserController.ioAsyncScope.launch {
//    browserController.saveSearchEngines()
//  }

  suspend fun loadMoreHistory(off: Int) {
    browserController.loadMoreHistory(off)
  }

  fun showToastMessage(message: String) {
    browserController.ioAsyncScope.launch { browserNMM.showToast(message) }
  }

  fun getDownloadModel() = browserController.downloadModel
}

/**
 * 根据内容解析成需要显示的内容
 */
internal fun parseInputText(text: String, needHost: Boolean = true): String {
  if (
  // deeplink
    text.startsWith("dweb:")
    // 内部页面
    || text.startsWith("about:") || text.startsWith("chrome:")
    // 域名
    || (text.isOnlyHost())
  ) {
    return text
  }
  // 尝试当成网址来解析
  val url = runCatching { Url(text) }.getOrElse { return text }
  // 判断是不是搜索引擎，如果是提取它的关键字
  for (item in DefaultAllWebEngine) {
    if (item.host == url.host) {
      return url.parameters[item.queryName()] ?: url.host
    }
  }

  return if (needHost && url.host.isNotEmpty()) {
    url.host
  } else text
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