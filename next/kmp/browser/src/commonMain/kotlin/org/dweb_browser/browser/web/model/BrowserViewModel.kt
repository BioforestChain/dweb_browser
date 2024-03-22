package org.dweb_browser.browser.web.model

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.search.SearchEngine
import org.dweb_browser.browser.search.SearchEngineList
import org.dweb_browser.browser.search.SearchInject
import org.dweb_browser.browser.search.ext.collectChannelOfEngines
import org.dweb_browser.browser.search.ext.isEngineAndGetHomeLink
import org.dweb_browser.browser.web.BrowserController
import org.dweb_browser.browser.web.data.AppBrowserTarget
import org.dweb_browser.browser.web.data.KEY_LAST_SEARCH_KEY
import org.dweb_browser.browser.web.data.KEY_NO_TRACE
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.browser.web.data.WebSiteType
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.browser.web.deepLinkDoSearch
import org.dweb_browser.browser.web.model.page.BrowserBookmarkPage
import org.dweb_browser.browser.web.model.page.BrowserDownloadPage
import org.dweb_browser.browser.web.model.page.BrowserHistoryPage
import org.dweb_browser.browser.web.model.page.BrowserHomePage
import org.dweb_browser.browser.web.model.page.BrowserPage
import org.dweb_browser.browser.web.model.page.BrowserSettingPage
import org.dweb_browser.browser.web.model.page.BrowserWebPage
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.isDwebDeepLink
import org.dweb_browser.helper.platform.toByteArray
import org.dweb_browser.helper.toWebUrl
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withScope
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermission
import org.dweb_browser.sys.share.ext.postSystemShare
import org.dweb_browser.sys.toast.PositionType
import org.dweb_browser.sys.toast.ext.showToast

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
 * 这里作为ViewModel
 */
class BrowserViewModel(
  private val browserController: BrowserController, internal val browserNMM: NativeMicroModule,
) {
  val ioScope get() = browserNMM.ioAsyncScope
  private val pages = mutableStateListOf<BrowserPage>() // 多浏览器列表
  val pageSize get() = pages.size

  var showMore by mutableStateOf(false)

  val previewPanelVisibleState = MutableTransitionState(PreviewPanelVisibleState.Close)

  enum class PreviewPanelVisibleState(val isVisible: Boolean) {
    DisplayGrid(true), Close(false), ;
  }

  /**
   * previewPanel 是否完成了布局计算，可以开始动画渲染
   */
  var previewPanelAnimationReady = mutableStateListOf<Int>()
  val showPreview get() = previewPanelVisibleState.targetState != PreviewPanelVisibleState.Close
  val isPreviewInvisible get() = !showPreview && previewPanelVisibleState.isIdle
  fun toggleShowPreviewUI(show: Boolean) {
    previewPanelVisibleState.targetState =
      if (show) PreviewPanelVisibleState.DisplayGrid else PreviewPanelVisibleState.Close
  }

  var showSearch by mutableStateOf<BrowserPage?>(null)
  var scale by mutableStateOf(1f)

  var showSearchEngine by mutableStateOf(false)

  // 无痕模式状态
  var isIncognitoOn by mutableStateOf(false)
    private set

  suspend fun updateIncognitoModeUI(noTrace: Boolean) {
    isIncognitoOn = noTrace
    withScope(ioScope) {
      browserController.saveStringToStore(KEY_NO_TRACE, if (noTrace) "true" else "")
    }
  }

  val searchInjectList = mutableStateListOf<SearchInject>()
  val searchEngineList = mutableStateListOf<SearchEngine>()
  val filterShowEngines get() = searchEngineList.filter { it.enable }
  fun findSearchEngine(url: String) = searchEngineList.firstOrNull { it.matchKeyWord(url) }

  suspend fun checkAndSearchUI(key: String, hide: () -> Unit) {
    val homeLink = withScope(ioScope) { browserNMM.isEngineAndGetHomeLink(key) } // 将关键字对应的搜索引擎置为有效
    debugBrowser("checkAndSearch", "homeLink=$homeLink")
    if (homeLink.isNotEmpty()) { // 使用首页地址直接打开网页
      hide()
      doSearchUI(homeLink)
    }
  }

  fun getBookmarks() = browserController.bookmarksStateFlow.value
  fun getHistoryLinks() = browserController.historyStateFlow.value
  val browserOnVisible = browserController.onWindowVisible
  val browserOnClose = browserController.onCloseWindow

  fun getPageOrNull(currentPage: Int) = pages.getOrNull(currentPage)
  fun getPage(currentPage: Int) = pages[currentPage]
  fun getPageIndex(page: BrowserPage) = pages.indexOf(page)

  var focusedPage by mutableStateOf<BrowserPage?>(null)
    private set
  val focusedPageIndex get() = pages.indexOf(focusedPage)

  private val focusPageChangeSignal: Signal<Pair<BrowserPage?, BrowserPage?>> = Signal()
  val onFocusedPageChangeUI = focusPageChangeSignal.toListener()
  suspend fun focusPageUI(page: BrowserPage?) {
    val prePage = focusedPage
    if (prePage == page) {
      return
    }
    debugBrowser("focusBrowserView", page)
    // 前一个页面要失去焦点了，所以进行截图
    prePage?.captureViewInBackground()
    focusedPage = page
    focusPageChangeSignal.emit(Pair(page, prePage))
  }

  suspend fun focusPageUI(pageIndex: Int) {
    focusPageUI(pages.getOrNull(pageIndex))
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

  val pagerStates = BrowserPagerStates(this)

  @Composable
  fun ViewModelEffect() {
    val uiScope = rememberCoroutineScope()
    /// 初始化 isNoTrace
    LaunchedEffect(Unit) {
      withScope(uiScope) {
        isIncognitoOn = browserController.getStringFromStore(KEY_NO_TRACE)?.isNotEmpty() ?: false
      }
    }

    /// 监听窗口关闭，进行资源释放
    DisposableEffect(Unit) {
      val off = browserController.onCloseWindow {
        withScope(uiScope) {
          pages.forEach { browserContentItem -> closePageUI(browserContentItem) }
          pages.clear()
        }
      }
      onDispose { off() }
    }

    /// 同步搜索引擎配置
    LaunchedEffect(Unit) {
      withScope(ioScope) {
        browserNMM.collectChannelOfEngines {
          withScope(uiScope) {
            searchEngineList.clear()
            searchEngineList.addAll(engineList)
          }
        }
      }
    }

    pagerStates.BindingEffect()
  }

  private suspend fun createDwebView(url: String) = IDWebView.create(
    browserNMM, DWebViewOptions(
      url = url,
      /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
      detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
    )
  )

  private suspend fun createWebPage(dWebView: IDWebView) =
    BrowserWebPage(dWebView, browserController).also {
      dWebView.onCreateWindow { itemDwebView ->
        val newWebPage = BrowserWebPage(itemDwebView, browserController)
        addNewPageUI(newWebPage)
      }
      dWebView.onDownloadListener { args: WebDownloadArgs ->
        debugBrowser("download", args)
        browserController.openDownloadView(args)
      }
    }

  private suspend fun createWebPage(url: String) = createWebPage(createDwebView(url))

  /**
   * 接收到deeplink的搜索或者打开网页请求操作
   */
  suspend fun openSearchPanelUI(url: String, target: AppBrowserTarget) {
    // 先判断search是否不为空，然后在判断search是否是地址，
    debugBrowser("openSearchPanelUI", "url=$url, target=$target")
    deepLinkDoSearch(DwebLinkSearchItem(link = url, target = target)) // 调用Ios 接口，实现功能
    // android 实现仍然在 commonMain这边
    when(target) {
      AppBrowserTarget.SELF -> {
        // 如果是搜索的话，直接在当前页显示搜索界面，并且显示待搜索的内容
//        if (url.isNotEmpty()) {
//          showSearch = focusedPage?.apply { searchKeyWord = url } // 显示当前界面
//        }
        tryOpenUrlUI(url) {
          debugBrowser("openSearchPanelUI", "tryOpenUrlUI fail, focusedPage=$focusedPage")
          val page = focusedPage?.let { tempPage ->
            if (tempPage is BrowserHomePage) tempPage.apply { searchKeyWord = url } else null
          } ?: run {
            BrowserHomePage(browserController).apply {
              searchKeyWord = url
              addNewPageUI(this) { addIndex = pageSize } // 直接移动到最后
            }
          }
          showSearch = page
        }
      }
      else -> {
        // 打开新tab，并聚焦到新page，如果打开地址失败，说明不是标准网址，新增tab，并且打开搜索界面
        tryOpenUrlUI(url) {
          debugBrowser("openSearchPanelUI", "tryOpenUrlUI fail, focusedPage=$focusedPage")
          val page = focusedPage?.let { tempPage -> // 如果当前界面就是homepage，就不需要再创建新的
            if (tempPage is BrowserHomePage) tempPage.apply { searchKeyWord = url } else null
          } ?: run {
            BrowserHomePage(browserController).apply {
              searchKeyWord = url
              addNewPageUI(this) { addIndex = pageSize } // 直接移动到最后
              focusPageUI(this)
            }
          }
          showSearch = page
        }
      }
    }
  }

  /**
   * 如果找到同样url的页面，那么聚焦那个页面
   * 如果没有找到，同时它又是合法的url，那么创建新页面打开
   * 否则，走 unknownUrl 回调
   *
   * > 为了适应 ios，从而将 webview 的处理独立开
   */
  suspend fun tryOpenUrlUI(
    url: String, replacePage: BrowserPage? = null, unknownUrl: (suspend (String) -> Unit)? = null
  ) {
    // 判断如果已存在，直接focus，不新增界面
    if (replacePage == null) {
      when (val samePage = pages.find { page ->
        page.isUrlMatch(url)
      }) {
        null -> {
          // 尝试添加新页面
          val newPage = addNewPageUI(url) { replaceOldHomePage = true }
          // 否则走未知模式
          if (newPage == null) {
            url.isNotEmpty().trueAlso { unknownUrl?.invoke(url) }
          } else {
            focusPageUI(newPage) // 发现通过 deeplink 请求时地址时，一直在首页，没有聚焦
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
        addNewPageUI(url) {
          addIndex = pages.indexOf(replacePage)
          replaceOldPage = true
        }
      }
    }
  }

  suspend fun closePageUI(page: BrowserPage): Boolean {
    val index = pages.indexOf(page)
    if (index == -1) {
      return false
    }
    if (focusedPage == page) {
      val newFocusIndex = maxOf(index + 1, pages.size - 1)
      if (newFocusIndex != index) {
        pages.getOrNull(newFocusIndex)?.also { focusPageUI(it) }
      }
    }

    /// 如果移除后，发现列表空了，手动补充一个。这个代码必须连着执行，否则会出问题
    pages.removeAt(index)
    if (pages.isEmpty()) {
      addNewPageUI(BrowserHomePage(browserController)) {
        focusPage = true
        replaceOldHomePage = false
      }
    }

    // 最后，将移除的页面进行销毁
    page.destroy()
    return true
  }

  suspend fun doSearchUI(url: String) {
    // 到搜索功能了，搜索引擎必须关闭
    showSearchEngine = false
    // 存储最后的搜索内容
    browserController.saveStringToStore(KEY_LAST_SEARCH_KEY, url)

    // 如果是 dweb_deeplink，直接代理访问。否则就是打开新页面
    if (url.isDwebDeepLink()) withScope(ioScope) { browserNMM.nativeFetch(url) }
    else addNewPageUI(url)
  }

  class AddPageOptions(
    var focusPage: Boolean = true,
    var addIndex: Int? = null,
    var replaceOldHomePage: Boolean = false,
    var replaceOldPage: Boolean = false,
  )

  suspend fun addNewPageUI(
    url: String? = null,
    options: AddPageOptions = AddPageOptions(),
    optionsModifier: (AddPageOptions.() -> Unit)? = null
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
    optionsModifier: (AddPageOptions.() -> Unit)? = null
  ) {
    optionsModifier?.invoke(options)

    val oldPage = options.addIndex?.let { index ->
      pages.getOrNull(index)?.also {
        pages.add(index, newPage)
      }
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
    ioScope.launch {
      browserController.saveBookLinks()
    }.join()
  }

  suspend fun addBookmarkUI(webPage: BrowserWebPage) =
    addBookmarkUI(webPage.webView.toWebSiteInfo(WebSiteType.Bookmark))


  suspend fun removeBookmarkUI(vararg items: WebSiteInfo) {
    showToastMessage(BrowserI18nResource.toast_message_remove_bookmark.text)
    browserController.bookmarksStateFlow.value -= items
    ioScope.launch {
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
    ioScope.launch { browserController.saveBookLinks() }.join()
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
    browserController.historyStateFlow.update { historyMap ->
      val dayList = historyMap[dayKey]?.filter { it.url != item.url } ?: return@update historyMap
      browserController.saveHistoryLinks(dayKey, dayList)
      historyMap + Pair(dayKey, dayList)
    }
  }

  suspend fun removeHistoryLink(items: List<WebSiteInfo>) {
    browserController.historyStateFlow.update { historyMap ->
      for (item in items) {
        val dayKey = item.day.toString()
        val dayList = historyMap[dayKey]?.filter { it.url != item.url } ?: continue
        browserController.saveHistoryLinks(dayKey, dayList)
        historyMap + Pair(dayKey, dayList)
      }
      historyMap
    }
  }

  suspend fun loadMoreHistory(off: Int) {
    browserController.loadMoreHistory(off)
  }

  fun showToastMessage(message: String, position: PositionType? = null) {
    browserController.ioScope.launch { browserNMM.showToast(message, position = position) }
  }
}

/**
 * 将合法的url，解析成需要显示的简要内容
 */
internal fun pageUrlTransformer(pageUrl: String, needHost: Boolean = true): String {
  if (
  // deeplink
    pageUrl.startsWith("dweb:")
    // 内部页面
    || pageUrl.startsWith("about:") || pageUrl.startsWith("chrome:")
    // android 特定的链接，理论上不应该给予支持
    || pageUrl.startsWith("file:///android_asset/")
  ) {
    return pageUrl
  }
  // 尝试当成网址来解析
  val url = pageUrl.toWebUrl() ?: return pageUrl

  // 判断是不是搜索引擎，如果是提取它的关键字
  for (item in SearchEngineList) {
    item.queryKeyWordValue(url)?.let { searchText ->
      return searchText
    }
  }
  return if (needHost && url.host.isNotEmpty()) {
    url.host.domainSimplify()
  } else pageUrl
}

/**
 * 尝试剔除 www.
 */
private fun String.domainSimplify() = if (startsWith("www.") && split('.').size == 3) {
  substring(4)
} else this

/**
 * 将WebViewState转为WebSiteInfo
 */
suspend fun IDWebView.toWebSiteInfo(type: WebSiteType, url: String = getUrl()) = WebSiteInfo(
  title = getTitle().ifEmpty { url },
  url = url,
  type = type,
  icon = getFavoriteIcon()?.toByteArray() // 这也有一个
)
