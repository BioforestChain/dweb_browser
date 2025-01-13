package org.dweb_browser.browser.web.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.ktor.http.Url
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.scan.openDeepLink
import org.dweb_browser.browser.search.SearchEngine
import org.dweb_browser.browser.search.SearchInject
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.BrowserNMM
import org.dweb_browser.browser.web.data.AppBrowserTarget
import org.dweb_browser.browser.web.data.KEY_NO_TRACE
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.data.WebSiteType
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.browser.web.deepLinkDoSearch
import org.dweb_browser.browser.web.model.page.BrowserBookmarkPage
import org.dweb_browser.browser.web.model.page.BrowserDownloadPage
import org.dweb_browser.browser.web.model.page.BrowserEnginePage
import org.dweb_browser.browser.web.model.page.BrowserHistoryPage
import org.dweb_browser.browser.web.model.page.BrowserHomePage
import org.dweb_browser.browser.web.model.page.BrowserPage
import org.dweb_browser.browser.web.model.page.BrowserSettingPage
import org.dweb_browser.browser.web.model.page.BrowserWebPage
import org.dweb_browser.browser.web.ui.BrowserPreviewPanel
import org.dweb_browser.browser.web.ui.BrowserSearchPanel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.readFile
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.clamp
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.format
import org.dweb_browser.helper.humanTrim
import org.dweb_browser.helper.isDwebDeepLink
import org.dweb_browser.helper.isTrimEndSlashEqual
import org.dweb_browser.helper.platform.toByteArray
import org.dweb_browser.helper.toWebUrl
import org.dweb_browser.helper.toWebUrlOrWithoutProtocol
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withScope
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermission
import org.dweb_browser.sys.share.ext.postSystemShare
import org.dweb_browser.sys.toast.ToastPositionType
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.WindowContentRenderScope

val LocalBrowserViewModel = compositionChainOf<BrowserViewModel>("BrowserModel")
val LocalShowIme = compositionChainOf("ShowIme") {
  mutableStateOf(false)
}

data class DwebLinkSearchItem(val link: String, val target: AppBrowserTarget) {
  companion object {
    val Empty = DwebLinkSearchItem("", AppBrowserTarget.SELF)
  }
}

/**
 * 这里作为 ViewModel
 */
class BrowserViewModel(
  internal val browserController: BrowserController,
  internal val browserNMM: BrowserNMM.BrowserRuntime,
) {
  val browserOnVisible = browserController.onWindowVisible // IOS 在用
  val browserOnClose = browserController.onCloseWindow // IOS 在用
  val lifecycleScope get() = browserNMM.getRuntimeScope()
  private val pages = mutableStateListOf<BrowserPage>() // 多浏览器列表
  val pageSize get() = pages.size
  var showMore by mutableStateOf(false)
  val previewPanel = BrowserPreviewPanel(this)
  val searchPanel = BrowserSearchPanel(this)

  // 该字段是用来存储通过 deeplink 调用的 search 和 openinbrowser 关键字，关闭搜索界面需要直接置空
  var searchKeyWord by mutableStateOf<String?>(null)
    internal set
  var scale by mutableFloatStateOf(1f)

  // 无痕模式状态
  var isIncognitoOn by mutableStateOf(false)
    private set

  suspend fun updateIncognitoModeUI(noTrace: Boolean) {
    isIncognitoOn = noTrace
    withScope(lifecycleScope) {
      browserController.saveStringToStore(KEY_NO_TRACE, if (noTrace) "true" else "")
    }
  }

  private var searchEngineList = listOf<SearchEngine>()
  val filterShowEngines get() = searchEngineList.filter { it.enable }
  val filterAllEngines get() = searchEngineList

  init {
    lifecycleScope.launch {
      // 同步搜索引擎列表
      browserNMM.collectChannelOfEngines {
        searchEngineList = engineList
      }
    }
  }

  val searchInjectList = mutableStateListOf<SearchInject>()

  /**获取注入的搜索引擎列表*/
  suspend fun getInjectList(searchText: String) {
    val list = browserNMM.getInjectList(searchText)
    searchInjectList.clear()
    searchInjectList.addAll(list)
  }

  suspend fun readFile(path: String) = browserNMM.readFile(path, create = false).binary()

  fun getBookmarks() = browserController.bookmarksStateFlow.value
  fun getHistoryLinks() = browserController.historyStateFlow.value

  fun getPageOrNull(currentPage: Int) = pages.getOrNull(currentPage)
  fun getPage(currentPage: Int) = getPageOrNull(currentPage) ?: pages.first()
  fun getPageIndex(page: BrowserPage) = pages.indexOf(page)
//  var focusedPage by mutableStateOf<BrowserPage?>(null)
//    private set
//  pagerStates
  var isTabFixedSize by mutableStateOf(true) // 用于标志当前的HorizontalPager中的PageSize是Fill还是Fixed
  val pagerStates = BrowserPagerStates(this)
  var focusedPage
    get() = pages.getOrNull(focusedPageIndex)
    set(value) {
      value?.also { focusedPageIndex = pages.indexOf(value) }
    }
  var focusedPageIndex
    get() = pagerStates.focusedPageIndex
    set(value) {
      pagerStates.focusedPageIndex = value
    }

  suspend fun focusPageUI(page: BrowserPage?) {
    val prePage = focusedPage
    if (prePage == page) {
      return
    }
    debugBrowser("focusBrowserView", page)
    // 前一个页面要失去焦点了，所以进行截图
    prePage?.captureViewInBackground("prePage cap by focus newPage")
    focusedPage = page
  }

  suspend fun focusPageUI(pageIndex: Int) {
    focusPageUI(pages.getOrNull(pageIndex))
  }

  /**
   * 请求系统权限
   */
  suspend fun requestSystemPermission(
    title: String = "", description: String = "", permissionName: SystemPermissionName,
  ): Boolean {
    return browserNMM.requestSystemPermission(
      SystemPermissionTask(
        permissionName, title = title, description = description
      )
    )
  }

  @Composable
  fun ViewModelEffect(windowRenderScope: WindowContentRenderScope) {
    val uiScope = rememberCoroutineScope()
    /// 初始化 isNoTrace
    LaunchedEffect(Unit) {
      withScope(uiScope) {
        isIncognitoOn = browserController.getStringFromStore(KEY_NO_TRACE)?.isNotEmpty() ?: false
      }
    }
    /// 判断是否需要显示SearchPanel
    LaunchedEffect(Unit) {
      snapshotFlow { searchKeyWord }.collect { keyWord ->
        val searchWord = keyWord?.ifEmpty { null } ?: return@collect
        val replaceHomePage = if (focusedPage is BrowserHomePage) focusedPage else null
        tryOpenUrlUI(searchWord, replaceHomePage) {
          // 如果searchWord不满足browserPage，那么就需要弹出搜索界面 BrowserSearchPanel
          val searchPage = focusedPage ?: run {
            BrowserHomePage(browserController).apply {
              addNewPageUI(this) { addIndex = focusedPageIndex + 1 } // 直接移动到最后
            }
          }
          searchPanel.showSearchPanel(searchPage)
        }
      }
    }
    /// 监听窗口关闭，进行资源释放
    DisposableEffect(Unit) {
      val off = browserController.onCloseWindow {
        withScope(uiScope) {
          pages.toList().forEach { browserContentItem -> closePageUI(browserContentItem) }
          pages.clear()
        }
      }
      onDispose { off() }
    }
  }

  private suspend fun createDwebView(url: String) = IDWebView.create(
    browserNMM, DWebViewOptions(
      // 主动补全 https:// 头
      url = url.toWebUrlOrWithoutProtocol()?.toString() ?: url,
      /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
      detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
      /// 桌面端web browser需要使用离屏渲染，才能preview tabs
      enabledOffScreenRender = false
    ), viewBox = browserController.viewBox
  ).also { dwebview ->
    browserNMM.onBeforeShutdown {
      browserNMM.scopeLaunch(cancelable = false) {
        dwebview.destroy()
      }
    }
  }

  private suspend fun createWebPage(dWebView: IDWebView): BrowserWebPage =
    BrowserWebPage(dWebView, browserController).also {
      dWebView.onCreateWindow { itemDwebView ->
        val url = itemDwebView.getUrl()
        if (!url.startsWith("dweb://")) {
          val newWebPage = createWebPage(itemDwebView)
          addNewPageUI(newWebPage)
        } else {
          itemDwebView.destroy()
        }
      }
      addDownloadListener(dWebView.onDownloadListener)
    }

  fun addDownloadListener(listener: Signal.Listener<WebDownloadArgs>) {
    listener.invoke { args: WebDownloadArgs ->
      debugBrowser("download", args)
      browserController.openDownloadDialog(args)
    }
  }

  private suspend fun createWebPage(url: String) = createWebPage(createDwebView(url))

  /**
   * 接收到 deeplink 的搜索或者打开网页请求操作
   */
  suspend fun openSearchPanelUI(searchText: String, target: AppBrowserTarget) {
    // 先判断search是否不为空，然后在判断search是否是地址，
    debugBrowser("openSearchPanelUI", "searchText=$searchText, target=$target")
    /// 目前iOS禁用了原生的浏览器实现，使用common，所以下面这行代码是走不过去的，因为等不到浏览器初始化
    if (false) {
      deepLinkDoSearch(DwebLinkSearchItem(link = searchText, target = target))
    }
    // android 实现仍然在 commonMain这边
    hideAllPanel() // 先将该内容置为空，然后修改 searchKeyWord 的值，出发 ViewModelEffect 监听，来确认是否需要再次显示
    searchKeyWord = searchText // 上面 ViewModelEffect 监听 searchKeyWord 状态，
  }

  /**
   * 如果找到同样url的页面，那么聚焦那个页面
   * 如果没有找到，同时它又是合法的url，那么创建新页面打开
   * 否则，走 unknownUrl 回调
   *
   * > 为了适应 ios，从而将 webview 的处理独立开
   */
  suspend fun tryOpenUrlUI(
    url: String, replacePage: BrowserPage? = null, unknownUrl: (suspend (String) -> Unit)? = null,
  ) {
    if (url.isEmpty()) return // 如果 url 是空的，直接返回，不操作
    // 判断如果已存在，直接focus，不新增界面
    if (replacePage == null) {
      when (val samePage = pages.find { page -> page.isUrlMatch(url) }) {
        null -> {
          // 尝试添加新页面
          val newPage = addNewPageUI(url) { replaceOldHomePage = true }
          // 否则走未知模式
          if (newPage == null) {
            unknownUrl?.invoke(url)
            return // 直接返回，否则会影响到将 searchKeyWord 置空操作，也就是下面调用的 hideAllPanel()
          }
        }

        else -> {
          focusPageUI(samePage)
          samePage.updateUrl(url)
        }
      }
    } else {
      if (replacePage is BrowserWebPage) {
        replacePage.updateUrl(url)
      } else if (replacePage.isUrlMatch(url)) {
        replacePage.updateUrl(url)
      } else {
        // 尝试添加新页面
        val newPage = addNewPageUI(url) {
          addIndex = pages.indexOf(replacePage)
          replaceOldPage = true
        }
        // 否则走未知模式
        if (newPage == null) {
          unknownUrl?.invoke(url)
          return // 直接返回，否则会影响到将 searchKeyWord 置空操作，也就是下面调用的 hideAllPanel()
        }
      }
    }
    hideAllPanel() // 为了保证页面加载的时候，将前面的Panel关闭
  }

  suspend fun closePageUI(page: BrowserPage): Boolean {
    val index = pages.indexOf(page)
    if (index == -1) {
      return false
    }

    return pages.remove(page).trueAlso {
      if (pages.isEmpty()) {
        addNewPageUI(BrowserHomePage(browserController)) {
          focusPage = true
          replaceOldHomePage = false
        }
      } else if (focusedPage == page || focusedPage == null) {
        // TODO 获取的index是否在移除后的列表有效区间内，如果存在可以进行聚焦，不存在（-1），等待PagerState内部自行处理
        focusPageUI(clamp(0, index, pageSize - 1))
      }
      page.destroy()
    }
  }

  /**检查是否有设置过的默认搜索引擎，并且拼接成webUrl*/
  private suspend fun checkAndEnableSearchEngine(key: String): Url? {
    val homeLink = withScope(lifecycleScope) {
      browserNMM.getEngineHomeLink(key.encodeURIComponent())
    } // 将关键字对应的搜索引擎置为有效
    return if (homeLink.isNotEmpty()) homeLink.toWebUrl() else null
  }

  fun doIOSearchUrl(searchEngine: SearchEngine, keyword: String) =
    doIOSearchUrl(searchEngine.searchLinks.first().format(keyword))

  /**
   * 判断 url 是否是 deepLink
   * 是：直接代理访问
   * 否：将 url 进行判断封装，符合条件后，判断当前界面是否是 BrowserWebPage，然后进行搜索操作
   */
  fun doIOSearchUrl(searchText: String) = lifecycleScope.launch {
    val text = searchText.humanTrim()
    if (text.isDwebDeepLink()) {
      browserNMM.nativeFetch(text)
      return@launch
    }
    // 尝试
    val webUrl = text.toWebUrl() ?: checkAndEnableSearchEngine(text) // 检查是否有默认的搜索引擎
    ?: text.toWebUrlOrWithoutProtocol() // 上面先判断标准的网址和搜索引擎后，仍然为空时，执行一次域名转化判断
    ?: filterShowEngines.firstOrNull()?.searchLinks?.first()?.format(text)?.toWebUrl() // 转换成搜索链接
    debugBrowser("doIOSearchUrl", "url=$text, webUrl=$webUrl, focusedPage=$focusedPage")
    // 当没有搜到需要的数据，给出提示
    webUrl?.toString()?.let { searchUrl ->
      if (focusedPage != null && focusedPage is BrowserWebPage) {
        (focusedPage as BrowserWebPage).loadUrl(searchUrl)// 使用当前页面继续搜索
      } else {
        addNewPageUI(searchUrl) { replaceOldPage = true } // 新增 BrowserWebPage 覆盖当前页
      }
    } ?: showToastMessage(BrowserI18nResource.Home.search_error.text)
  }

  data class AddPageOptions(
    var focusPage: Boolean = true,
    var addIndex: Int? = null,
    var replaceOldHomePage: Boolean = false,
    var replaceOldPage: Boolean = false,
  )

  suspend fun addNewPageUI(
    url: String? = null,
    options: AddPageOptions = AddPageOptions(),
    optionsModifier: (AddPageOptions.() -> Unit)? = null,
  ): BrowserPage? {
    val newPage = if (url == null || BrowserHomePage.isNewTabUrl(url)) {
      BrowserHomePage(browserController)
    } else if (BrowserBookmarkPage.isBookmarkUrl(url)) {
      BrowserBookmarkPage(browserController)
    } else if (BrowserHistoryPage.isHistoryUrl(url)) {
      BrowserHistoryPage(browserController)
    } else if (BrowserDownloadPage.isDownloadUrl(url)) {
      BrowserDownloadPage(browserController)
    } else if (BrowserSettingPage.isSettingUrl(url)) {
      BrowserSettingPage(browserController)
    } else if (BrowserEnginePage.isEngineUrl(url)) {
      BrowserEnginePage(browserController)
    } else if (BrowserWebPage.isWebUrl(url)) { // 判断是否网页应该放在最下面
      createWebPage(url)
    } else null
    if (newPage != null) {
      addNewPageUI(newPage, options, optionsModifier)
    }
    return newPage
  }

  private suspend fun addNewPageUI(
    newPage: BrowserPage,
    options: AddPageOptions = AddPageOptions(),
    optionsModifier: (AddPageOptions.() -> Unit)? = null,
  ) {
    optionsModifier?.invoke(options)
    val oldPage = options.addIndex?.let { index ->
      pages.getOrNull(index)?.also { pages.add(index, newPage) }
    } ?: focusedPage.also { pages.add(newPage) }

    if ((options.replaceOldPage && oldPage != null) || (options.replaceOldHomePage && oldPage != focusedPage && oldPage is BrowserHomePage)) {
      closePageUI(oldPage)
    }
    if (options.focusPage) {
      focusPageUI(newPage)
    }
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
  suspend fun addUrlToDesktopUI(): Boolean {
    return this.focusedPage?.let { page ->
      if (page is BrowserWebPage) {
        addUrlToDesktopUI(page)
      } else false
    } ?: false
  }

  suspend fun addUrlToDesktopUI(page: BrowserWebPage) = addUrlToDesktopUI(
    title = page.title, link = page.url, iconString = page.webView.getIcon()
  )

  suspend fun addUrlToDesktopUI(title: String, link: String, iconString: String): Boolean {
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

  /**
   * 操作书签数据
   * 新增：需要新增数据
   * 修改：该对象已经变更，可直接保存，所以不需要传
   * 删除：需要删除数据
   */
  suspend fun addBookmarkUI(vararg items: WebSiteInfo) {
    showToastMessage(BrowserI18nResource.toast_message_add_bookmark.text)
    val oldBookmarkMap = browserController.bookmarksStateFlow.value.associateBy { it.url }
    // 在老列表中，寻找没有交集的部分
    val newItems = items.filter { newItem -> !oldBookmarkMap.containsKey(newItem.url) }
    // 追加到前面
    browserController.bookmarksStateFlow.value =
      (newItems + browserController.bookmarksStateFlow.value)
    lifecycleScope.launch {
      browserController.saveBookLinks()
    }.join()
  }

  suspend fun addBookmarkUI(webPage: BrowserWebPage) =
    addBookmarkUI(webPage.webView.toWebSiteInfo(WebSiteType.Bookmark))

  suspend fun removeBookmarkUI(vararg items: WebSiteInfo) {
    showToastMessage(BrowserI18nResource.toast_message_remove_bookmark.text)
    browserController.bookmarksStateFlow.value -= items
    lifecycleScope.launch {
      browserController.saveBookLinks()
    }.join()
  }

  suspend fun removeBookmarkUI(url: String) = browserController.bookmarksStateFlow.value.filter {
    it.url == url
  }.map { removeBookmarkUI(it) }

  /**
   * 修改书签
   *
   * 返回Boolean：是否修改成功
   */
  suspend fun updateBookmarkUI(oldBookmark: WebSiteInfo, newBookmark: WebSiteInfo): Boolean {
    val bookmarks = browserController.bookmarksStateFlow.value
    val index = bookmarks.indexOf(oldBookmark)
    if (index == -1) {
      return false
    }
    val newBookmarks = bookmarks.toMutableList()
    newBookmarks[index] = newBookmark
    browserController.bookmarksStateFlow.value = newBookmarks.toList()
    showToastMessage(BrowserI18nResource.toast_message_update_bookmark.text)
    lifecycleScope.launch { browserController.saveBookLinks() }.join()
    return true
  }

  /**
   * 操作历史数据
   * 新增：需要新增数据
   * 修改：历史数据没有修改
   * 删除：需要删除数据
   */
  suspend fun addHistoryLinkUI(item: WebSiteInfo) {
    if (isIncognitoOn) return // 如果是无痕模式，则不能进行存储历史操作
    val dayKey = item.day.toString()
    val addUrl = item.url
    browserController.historyStateFlow.update { historyMap ->
      val dayList = historyMap[dayKey]?.run {
        toMutableList().apply {
          removeAll { item -> item.url.isTrimEndSlashEqual(addUrl) } // 删除同一天的重复数据
          add(0, item)
        }.toList()
      } ?: listOf(item)
      browserController.saveHistoryLinks(dayKey, dayList)

      historyMap + Pair(dayKey, dayList)
    }
  }

  suspend fun removeHistoryLink(item: WebSiteInfo) {
    val dayKey = item.day.toString()
    browserController.historyStateFlow.update { historyMap ->
      val dayList = historyMap[dayKey]?.filter { it.url != item.url } ?: return@update historyMap
      browserController.saveHistoryLinks(dayKey, dayList)
      historyMap + Pair(dayKey, dayList)
    }
  }

  suspend fun removeHistoryLink(items: List<WebSiteInfo>) {
    browserController.historyStateFlow.update { historyMap ->
      var newHistoryMap = historyMap
      val removeMap = items.groupBy { it.day.toString() }
      removeMap.map { (dayKey, list) ->
        historyMap[dayKey]?.filterNot { it in list }?.also { dayList ->
          browserController.saveHistoryLinks(dayKey, dayList)
          newHistoryMap = historyMap + Pair(dayKey, dayList)
        }
      }
      newHistoryMap
    }
  }

  suspend fun loadMoreHistory(off: Int) {
    browserController.loadMoreHistory(off)
  }

  fun showToastMessage(message: String, position: ToastPositionType? = null) {
    browserController.lifecycleScope.launch { browserNMM.showToast(message, position = position) }
  }

  fun disableSearchEngine(searchEngine: SearchEngine) = lifecycleScope.launch {
    browserNMM.updateEngineState(searchEngine, false)
  }

  fun enableSearchEngine(searchEngine: SearchEngine) = lifecycleScope.launch {
    browserNMM.updateEngineState(searchEngine, true)
  }

  fun showQRCodePanelUI() {
    browserNMM.scopeLaunch(cancelable = true) {
      val response = browserNMM.nativeFetch("file://scan.browser.dweb/open")
      if (response.isOk) {
        openDeepLink(response.text())
      } else {
        browserNMM.showToast(message = response.text())
      }
    }
  }

  /**
   * 隐藏所有的Panel
   */
  fun hideAllPanel() {
    previewPanel.hideBrowserPreviewWithoutAnimation()
    searchKeyWord = null
    searchPanel.hideSearchPanel()
  }
}

/**
 * 将WebViewState转为WebSiteInfo
 */
suspend fun IDWebView.toWebSiteInfo(type: WebSiteType, url: String = getUrl()) = WebSiteInfo(
  title = getTitle().ifEmpty { url },
  url = url,
  type = type,
  icon = getIconBitmap()?.toByteArray() // 这也有一个
)
