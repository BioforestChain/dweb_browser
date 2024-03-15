package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.barcode.LocalQRCodeModel
import org.dweb_browser.browser.common.barcode.QRCodeScanModel
import org.dweb_browser.browser.common.barcode.QRCodeScanView
import org.dweb_browser.browser.common.barcode.QRCodeState
import org.dweb_browser.browser.common.barcode.openDeepLink
import org.dweb_browser.browser.util.isSystemUrl
import org.dweb_browser.browser.web.data.ConstUrl
import org.dweb_browser.browser.web.data.page.BrowserPage
import org.dweb_browser.browser.web.data.page.BrowserWebPage
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserPageState
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.LocalInputText
import org.dweb_browser.browser.web.model.LocalShowIme
import org.dweb_browser.browser.web.model.LocalShowSearchView
import org.dweb_browser.browser.web.model.parseInputText
import org.dweb_browser.browser.web.ui.page.BrowserHomePageRender
import org.dweb_browser.dwebview.rememberLoadingProgress
import org.dweb_browser.helper.capturable.capturable
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

internal val dimenTextFieldFontSize = 16.sp
internal val dimenSearchHorizontalAlign = 5.dp
internal val dimenSearchVerticalAlign = 10.dp
internal val dimenSearchRoundedCornerShape = 8.dp
internal val dimenShadowElevation = 4.dp
internal val dimenHorizontalPagerHorizontal = 20.dp
internal val dimenBottomHeight = 100.dp
internal val dimenSearchHeight = 40.dp
internal val dimenNavigationHeight = 40.dp

@Composable
fun BrowserViewForWindow(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowRenderScope
) {
  val scope = rememberCoroutineScope()
  val browserPagerState = viewModel.rememberBrowserPagerState()
  val qrCodeScanModel = remember { QRCodeScanModel() }
  val showSearchView = LocalShowSearchView.current
  val focusManager = LocalFocusManager.current

  viewModel.BrowserSearchConfig() // 用于控制是否显示搜索框

  LocalCompositionChain.current.Provider(
    LocalBrowserViewModel provides viewModel,
    LocalBrowserPageState provides browserPagerState,
    LocalQRCodeModel provides qrCodeScanModel,
  ) {
    // 窗口 BottomSheet 的按钮
    val win = LocalWindowController.current

//    // 窗口级返回操作
//    win.GoBackHandler {
//      val browserContentItem = viewModel.focusPage ?: return@GoBackHandler
//      scope.launch {
//        if (showSearchView.value) { // 如果显示搜索界面，优先关闭搜索界面
//          focusManager.clearFocus()
//          showSearchView.value = false
//        } else if (viewModel.showPreview) {
//          viewModel.updatePreviewState(false)
//        } else if (qrCodeScanModel.state.value != QRCodeState.Hide) {
//          qrCodeScanModel.state.value = QRCodeState.Hide
//        } else {
//          browserContentItem.contentWebItem.value?.let { contentWebItem ->
//            if (contentWebItem.viewItem.webView.canGoBack()) {
//              contentWebItem.viewItem.webView.goBack()
//            } else {
//              viewModel.closePage(browserContentItem)
//            }
//          } ?: win.hide()
//        }
//      }
//    }

    val calculateModifier = with(windowRenderScope) {
      if (win.isMaximized()) {
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
      } else {
        modifier.requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
          .scale(scale).background(MaterialTheme.colorScheme.background)
      }
    }

    Box(modifier = calculateModifier) {
      Box(modifier = Modifier.padding(bottom = dimenBottomHeight * windowRenderScope.scale)) {
        BrowserPageBox(viewModel, windowRenderScope)   // 中间网页主体
      }
      BrowserBottomBar(viewModel) // 工具栏，包括搜索框和导航栏
      BrowserPreviewPanel(viewModel)// 用于显示多界面
      // 搜索界面考虑到窗口和全屏问题，显示的问题，需要控制modifier
      BrowserSearchView(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        windowRenderScope = windowRenderScope
      )
      QRCodeScanView(onSuccess = {
        openDeepLink(it)
        scope.launch { qrCodeScanModel.stateChange.emit(QRCodeState.Hide) }
      }, onCancel = { scope.launch { qrCodeScanModel.stateChange.emit(QRCodeState.Hide) } })
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserPageBox(viewModel: BrowserViewModel, windowRenderScope: WindowRenderScope) {
  val localFocusManager = LocalFocusManager.current
  val browserPagerState = LocalBrowserPageState.current

  UpdateHorizontalPager(viewModel)

  Box(modifier = Modifier.fillMaxSize().clickableWithNoEffect {
    localFocusManager.clearFocus()
  }) {
    HorizontalPager(modifier = Modifier,
      state = browserPagerState.pagerStateContent,
      pageSpacing = 0.dp,
      userScrollEnabled = false,
      reverseLayout = false,
      contentPadding = PaddingValues(0.dp),
      beyondBoundsPageCount = 1,
      pageContent = { currentPage ->
        viewModel.getBrowserViewOrNull(currentPage)?.also { browserPage ->
          browserPage.scale = windowRenderScope.scale
          browserPage.Render(Modifier.capturable(browserPage.captureController))
          LocalWindowController.current.GoBackHandler {
            viewModel.closePage(browserPage)
          }
        }
      })
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpdateHorizontalPager(viewModel: BrowserViewModel) {
  val pagerStateNavigator = LocalBrowserPageState.current.pagerStateNavigator
  val pagerStateContent = LocalBrowserPageState.current.pagerStateContent
  LaunchedEffect(pagerStateNavigator.currentPageOffsetFraction) {
    val lastCurrentPage = pagerStateNavigator.currentPage
    val lastCurrentPageOffsetFraction = pagerStateNavigator.currentPageOffsetFraction
    /** 由于HorizontalPager的有效区间值是 -0.5f~0.5f ,荣耀手机在这块兼容出问题了，导致出现了不在区间的值，
     * 所以在这边强制限制值必须在 -0.5f~0.5f 之间
     */
    val (currentPage, currentPageOffsetFraction) = if (lastCurrentPageOffsetFraction >= 0.5f) {
      Pair(lastCurrentPage + 1, 1 - lastCurrentPageOffsetFraction)
    } else if (lastCurrentPageOffsetFraction <= -0.5f) {
      Pair(lastCurrentPage - 1, -1 - lastCurrentPageOffsetFraction)
    } else Pair(lastCurrentPage, lastCurrentPageOffsetFraction)
    pagerStateContent.scrollToPage(
      currentPage, currentPageOffsetFraction
    )
  }
  LaunchedEffect(pagerStateContent.currentPage) {
    val currentPage = pagerStateContent.currentPage
    viewModel.updateCurrentBrowserView(currentPage)
  }
  /**
   * 为了截图，判断 pagerStateNavigator 值
   */
  LaunchedEffect(pagerStateNavigator.targetPage) {
    viewModel.capturePage(pagerStateNavigator.targetPage)
  }
}

@Composable
fun BoxScope.BrowserBottomBar(viewModel: BrowserViewModel) {
  Column(
    modifier = Modifier.fillMaxWidth()
      .background(MaterialTheme.colorScheme.background)
      .align(Alignment.BottomCenter)
  ) {
    BrowserSearchBar(viewModel)
    BrowserNavigatorBar(viewModel)
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserSearchBar(viewModel: BrowserViewModel) {
  val pagerStateNavigator = LocalBrowserPageState.current.pagerStateNavigator
  val localShowIme = LocalShowIme.current

  /*LaunchedEffect(pagerStateNavigator.settledPage) { // 为了修复隐藏搜索框后，重新加载时重新显示的问题，会显示第一页
    delay(100)
    pagerStateNavigator.scrollToPage(pagerStateNavigator.settledPage)
  }*/
  val localFocus = LocalFocusManager.current
  LaunchedEffect(Unit) {
    viewModel.showMore
    if (!localShowIme.value && !viewModel.showSearchEngine) {
      localFocus.clearFocus()
    }
  }

  HorizontalPager(
    modifier = Modifier,
    state = pagerStateNavigator,
    pageSpacing = 0.dp,
    userScrollEnabled = true,
    reverseLayout = false,
    contentPadding = PaddingValues(horizontal = dimenHorizontalPagerHorizontal),
    beyondBoundsPageCount = 5,
    pageContent = { currentPage ->
      SearchBox(viewModel.getBrowserViewOrNull(currentPage)!!)
    },
  )
}


@Composable
private fun SearchBox(page: BrowserPage) {
  var showSearchView by LocalShowSearchView.current
  val searchHint = BrowserI18nResource.browser_search_hint()

  Box(modifier = Modifier.padding(
    horizontal = dimenSearchHorizontalAlign, vertical = dimenSearchVerticalAlign
  ).fillMaxWidth()
    .shadow(
      elevation = dimenShadowElevation,
      shape = RoundedCornerShape(dimenSearchRoundedCornerShape)
    )
    .height(dimenSearchHeight)
    .clip(RoundedCornerShape(dimenSearchRoundedCornerShape))
    .background(MaterialTheme.colorScheme.surface)
    .clickable { showSearchView = true }) {
    if (page is BrowserWebPage) {
      ShowLinearProgressIndicator(page)
    }
    val inputText = page.url
    val (title, align, icon) = if (inputText.isEmpty() || inputText.isSystemUrl()) {
      Triple(searchHint, TextAlign.Start, Icons.Default.Search)
    } else {
      Triple(parseInputText(inputText), TextAlign.Center, Icons.Default.FormatSize)
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).align(Alignment.Center),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (page.icon != null) {

      }
      Icon(icon, contentDescription = "Search")
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = title,
        textAlign = align,
        fontSize = dimenTextFieldFontSize,
        maxLines = 1,
        modifier = Modifier.weight(1f)
      )
    }
  }
}

/**
 * 用于显示 WebView 加载进度
 */
@Composable
private fun BoxScope.ShowLinearProgressIndicator(page: BrowserWebPage) {
  when (val loadingProgress = page.webView.rememberLoadingProgress()) {
    0f, 1f -> {}
    else -> {
      LinearProgressIndicator(
        progress = { loadingProgress },
        modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

/**
 * 提供给外部调用的  搜索界面，可以含有BrowserViewModel
 */
@Composable
fun BrowserSearchView(
  viewModel: BrowserViewModel, modifier: Modifier = Modifier, windowRenderScope: WindowRenderScope
) {
  var showSearchView by LocalShowSearchView.current
  val searchHint = BrowserI18nResource.browser_search_hint()
  val focusManager = LocalFocusManager.current
  if (showSearchView) {
    val dwebLink = viewModel.dwebLinkSearch.value.link
    val inputText = if (dwebLink.trim().isEmpty() || dwebLink == ConstUrl.BLANK.url) {
      viewModel.focusPage?.url ?: ""
    } else dwebLink
    val showText = if (inputText.isSystemUrl() || inputText == searchHint) "" else inputText

    val inputTextState = LocalInputText.current

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
      .clickableWithNoEffect {
        focusManager.clearFocus()
        showSearchView = false
      }) {
      SearchView(text = showText,
        modifier = modifier,
        homePreview = { BrowserHomePageRender() },
        onClose = {
          showSearchView = false
        },
        onSearch = { url -> // 第一个是搜索关键字，第二个是搜索地址
          viewModel.doSearch(url)
          inputTextState.value = url
          showSearchView = false
        })
    }
  }
}

/*@Composable
internal fun HomeWebviewPage(
  viewModel: BrowserViewModel,
  windowRenderScope: WindowRenderScope,
  onClickOrMove: (Boolean) -> Unit
) {
  var _webView by remember {
    mutableStateOf<BrowserWebView?>(null)
  }
  LaunchedEffect(Unit) {
    _webView = viewModel.searchBackBrowserView.await()
  }
  val webView = _webView ?: return

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .padding(bottom = dimenSearchHeight * windowRenderScope.scale).background(Color.Red)
  ) {
    webView.viewItem.webView.Render(
      Modifier.fillMaxSize(),
    )

    val density = LocalDensity.current.density
    LaunchedEffect(windowRenderScope.scale, maxWidth, maxHeight) {
      webView.viewItem.webView.setContentScale(
        windowRenderScope.scale,
        maxWidth.value,
        maxHeight.value,
        density
      )
    }
  }
}*/