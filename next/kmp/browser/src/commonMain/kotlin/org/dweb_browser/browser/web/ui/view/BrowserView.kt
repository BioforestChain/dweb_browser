package org.dweb_browser.browser.web.ui.view

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.Filter1
import androidx.compose.material.icons.rounded.Filter2
import androidx.compose.material.icons.rounded.Filter3
import androidx.compose.material.icons.rounded.Filter4
import androidx.compose.material.icons.rounded.Filter5
import androidx.compose.material.icons.rounded.Filter6
import androidx.compose.material.icons.rounded.Filter7
import androidx.compose.material.icons.rounded.Filter8
import androidx.compose.material.icons.rounded.Filter9
import androidx.compose.material.icons.rounded.Filter9Plus
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.dweb_browser.browser.web.model.BrowserContentItem
import org.dweb_browser.browser.web.model.ConstUrl
import org.dweb_browser.browser.web.ui.model.LocalModalBottomSheet
import org.dweb_browser.browser.web.ui.model.ModalBottomModel
import org.dweb_browser.browser.web.ui.model.SheetState
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.model.LocalBrowserModel
import org.dweb_browser.browser.web.ui.model.LocalBrowserPageState
import org.dweb_browser.browser.web.ui.model.LocalInputText
import org.dweb_browser.browser.web.ui.model.LocalShowIme
import org.dweb_browser.browser.web.ui.model.LocalShowSearchView
import org.dweb_browser.browser.web.ui.model.parseInputText
import org.dweb_browser.dwebview.rememberCanGoBack
import org.dweb_browser.dwebview.rememberLoadingProgress
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
internal val dimenMinBottomHeight = 20.dp

private val bottomEnterAnimator = slideInVertically(animationSpec = tween(300),//动画时长1s
  initialOffsetY = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  })
private val bottomExitAnimator = slideOutVertically(animationSpec = tween(300),//动画时长1s
  targetOffsetY = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  })

@Composable
fun BrowserViewForWindow(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowRenderScope
) {
  val scope = rememberCoroutineScope()
  val browserPagerState = viewModel.rememberBrowserPagerState()
  val modalBottomModel = remember { ModalBottomModel() }
  val qrCodeScanModel = remember { QRCodeScanModel() }
  val showSearchView = LocalShowSearchView.current
  val focusManager = LocalFocusManager.current

  viewModel.BrowserSearchConfig() // 用于控制是否显示搜索框

  LocalCompositionChain.current.Provider(
    LocalBrowserModel provides viewModel,
    LocalModalBottomSheet provides modalBottomModel,
    LocalBrowserPageState provides browserPagerState,
    LocalQRCodeModel provides qrCodeScanModel,
  ) {
    // 窗口 BottomSheet 的按钮
    val win = LocalWindowController.current
    fun backHandler() {
      val browserContentItem = viewModel.currentTab ?: return
      scope.launch {
        if (showSearchView.value) { // 如果显示搜索界面，优先关闭搜索界面
          focusManager.clearFocus()
          showSearchView.value = false
        } else if (modalBottomModel.state.value != SheetState.Hidden) {
          modalBottomModel.hide()
        } else if (viewModel.showMultiView.targetState) {
          viewModel.updateMultiViewState(false)
        } else if (qrCodeScanModel.state.value != QRCodeState.Hide) {
          qrCodeScanModel.state.value = QRCodeState.Hide
        } else {
          browserContentItem.contentWebItem.value?.let { contentWebItem ->
            if (contentWebItem.viewItem.webView.historyCanGoBack()) {
              contentWebItem.viewItem.webView.historyGoBack()
            } else {
              viewModel.closeWebView(browserContentItem)
            }
          } ?: win.hide()
        }
      }
    }

    // 窗口级返回操作
    win.GoBackHandler { backHandler() }

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = dimenBottomHeight * windowRenderScope.scale)
      ) {
        BrowserViewContent(viewModel, windowRenderScope)   // 中间主体部分
      }
      Box(modifier = with(windowRenderScope) {
        if (win.isMaximized()) {
          Modifier.fillMaxSize()
        } else {
          Modifier
            .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
            .scale(scale)
        }
      }) {
        BrowserViewBottomBar(viewModel) // 工具栏，包括搜索框和导航栏
        BrowserMultiPopupView(viewModel)// 用于显示多界面
        // 搜索界面考虑到窗口和全屏问题，显示的问题，需要控制modifier
        BrowserSearchView(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
          windowRenderScope = windowRenderScope
        )
        BrowserBottomSheet(viewModel)
        QRCodeScanView(
          onSuccess = {
            openDeepLink(it)
            scope.launch { qrCodeScanModel.stateChange.emit(QRCodeState.Hide) }
          },
          onCancel = { scope.launch { qrCodeScanModel.stateChange.emit(QRCodeState.Hide) } }
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserViewContent(viewModel: BrowserViewModel, windowRenderScope: WindowRenderScope) {
  val localFocusManager = LocalFocusManager.current
  val browserPagerState = LocalBrowserPageState.current

  UpdateHorizontalPager(viewModel)

  Box(modifier = Modifier
    .fillMaxSize()
    .clickableWithNoEffect {
      localFocusManager.clearFocus()
    }
  ) {
    HorizontalPager(modifier = Modifier,
      state = browserPagerState.pagerStateContent,
      pageSpacing = 0.dp,
      userScrollEnabled = false,
      reverseLayout = false,
      contentPadding = PaddingValues(0.dp),
      beyondBoundsPageCount = 5,
      pageContent = { currentPage ->
        BrowserViewContentWeb(
          viewModel,
          viewModel.getBrowserViewOrNull(currentPage)!!,
          windowRenderScope
        )
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
      currentPage,
      currentPageOffsetFraction
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
    viewModel.captureBrowserWebView(pagerStateNavigator.targetPage)
  }
}

@Composable
fun BoxScope.BrowserViewBottomBar(viewModel: BrowserViewModel) {
  Box(
    modifier = Modifier.align(Alignment.BottomCenter)
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      BrowserViewSearch(viewModel)
      BrowserViewNavigatorBar(viewModel)
    }
    // 小标题暂时不需要，先屏蔽
    /*Column(modifier = Modifier
      .fillMaxWidth()
      .height(dimenMinBottomHeight)
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .align(Alignment.BottomCenter)
      .clickable { viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(true)) }) {
      MiniTitle(viewModel)
    }

    AnimatedVisibility(
      visibleState = viewModel.uiState.showBottomBar,
      enter = bottomEnterAnimator,
      exit = bottomExitAnimator,
      modifier = Modifier.background(MaterialTheme.colorScheme.background) // surfaceVariant
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        BrowserViewSearch(viewModel)
        BrowserViewNavigatorBar(viewModel)
      }
    }*/
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserViewSearch(viewModel: BrowserViewModel) {
  val pagerStateNavigator = LocalBrowserPageState.current.pagerStateNavigator
  val localShowIme = LocalShowIme.current

  /*LaunchedEffect(pagerStateNavigator.settledPage) { // 为了修复隐藏搜索框后，重新加载时重新显示的问题，会显示第一页
    delay(100)
    pagerStateNavigator.scrollToPage(pagerStateNavigator.settledPage)
  }*/
  val localFocus = LocalFocusManager.current
  LaunchedEffect(Unit) {
    if (!localShowIme.value && !viewModel.showSearchEngine.targetState) {
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
private fun BrowserViewNavigatorBar(viewModel: BrowserViewModel) {
  val scope = rememberCoroutineScope()
  val bottomSheetModel = LocalModalBottomSheet.current
  val qrCodeScanState = LocalQRCodeModel.current
  val browserContentItem = viewModel.currentTab ?: return
  key(browserContentItem) {
    val webView = browserContentItem.contentWebItem.value?.viewItem?.webView
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(dimenNavigationHeight)
    ) {
      val canGoBack = webView?.rememberCanGoBack() ?: false
      val isSystemUrl = webView?.getUrl()?.isSystemUrl() ?: true

      NavigatorButton(
        imageVector = Icons.Rounded.AddHome,
        name = "AddHome",
        show = !isSystemUrl
      ) {
        scope.launch { viewModel.addUrlToDesktop() }
      }
      NavigatorButton(
        imageVector = if (canGoBack) Icons.Rounded.Add else Icons.Rounded.QrCodeScanner,
        name = if (canGoBack) "Add" else "Scan",
        show = true
      ) {
        scope.launch {
          if (canGoBack) {
            viewModel.addNewMainView()
          } else {
            qrCodeScanState.stateChange.emit(QRCodeState.Scanning)
          }
        }
      }
      NavigatorButton(
        imageVector = getMultiImageVector(viewModel.listSize), // resId = R.drawable.ic_main_multi,
        name = "MultiView", show = true
      ) {
        scope.launch {
          viewModel.currentTab?.captureView()
          viewModel.updateMultiViewState(true)
        }
      }
      NavigatorButton(
        imageVector = Icons.Rounded.Menu, name = "Options", show = true
      ) {
        scope.launch { bottomSheetModel.show() }
      }
    }
  }
}

private fun getMultiImageVector(size: Int) = when (size) {
  1 -> Icons.Rounded.Filter1
  2 -> Icons.Rounded.Filter2
  3 -> Icons.Rounded.Filter3
  4 -> Icons.Rounded.Filter4
  5 -> Icons.Rounded.Filter5
  6 -> Icons.Rounded.Filter6
  7 -> Icons.Rounded.Filter7
  8 -> Icons.Rounded.Filter8
  9 -> Icons.Rounded.Filter9
  else -> Icons.Rounded.Filter9Plus
}

@Composable
private fun RowScope.NavigatorButton(
  imageVector: ImageVector, name: String, show: Boolean, onClick: () -> Unit
) {
  Box(modifier = Modifier
    .weight(1f)
    .fillMaxHeight()
    .padding(horizontal = 2.dp)
    .clickable(enabled = show) { onClick() }) {
    Column(modifier = Modifier.align(Alignment.Center)) {
      Icon(
        modifier = Modifier.size(28.dp),
        imageVector = imageVector, //ImageVector.vectorResource(id = resId),//ImageBitmap.imageResource(id = resId),
        contentDescription = name,
        tint = if (show) {
          MaterialTheme.colorScheme.onSecondaryContainer
        } else {
          MaterialTheme.colorScheme.outlineVariant
        }
      )
    }
  }
}

@Composable
private fun BrowserViewContentWeb(
  viewModel: BrowserViewModel,
  browserContentItem: BrowserContentItem,
  windowRenderScope: WindowRenderScope
) {
  browserContentItem.contentWebItem.value?.let { contentWebItem ->
    key(contentWebItem.viewItem.webviewId) {
      BrowserWebView(
        viewModel = viewModel,
        browserContentItem = browserContentItem,
        windowRenderScope = windowRenderScope
      )
    }
  } ?: BrowserMainView(viewModel, browserContentItem)
}

@Composable
private fun SearchBox(browserContentItem: BrowserContentItem) {
  var showSearchView by LocalShowSearchView.current
  val searchHint = BrowserI18nResource.browser_search_hint()
  val contentWebItem = browserContentItem.contentWebItem.value

  var inputText by remember { mutableStateOf("") }
  contentWebItem?.let {
    inputText = contentWebItem.viewItem.webView.getUrl()
    DisposableEffect(Unit) {
      val off = contentWebItem.viewItem.webView.onReady {
        inputText = it
      }
      onDispose { off() }
    }
  } ?: run { inputText = "" }

  Box(modifier = Modifier
    .padding(
      horizontal = dimenSearchHorizontalAlign, vertical = dimenSearchVerticalAlign
    )
    .fillMaxWidth()
    .shadow(
      elevation = dimenShadowElevation, shape = RoundedCornerShape(dimenSearchRoundedCornerShape)
    )
    .height(dimenSearchHeight)
    .clip(RoundedCornerShape(dimenSearchRoundedCornerShape))
    .background(MaterialTheme.colorScheme.surface)
    .clickable { showSearchView = true }
  ) {
    ShowLinearProgressIndicator(contentWebItem)

    val (title, align, icon) = if (inputText.isEmpty() || inputText.isSystemUrl()) {
      Triple(searchHint, TextAlign.Start, Icons.Default.Search)
    } else {
      Triple(
        parseInputText(inputText), TextAlign.Center, Icons.Default.FormatSize
      )
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp)
        .align(Alignment.Center),
      verticalAlignment = Alignment.CenterVertically
    ) {
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
private fun BoxScope.ShowLinearProgressIndicator(contentWebItem: BrowserContentItem.ContentWebItem?) {
  contentWebItem?.let {
    when (val loadingProgress = contentWebItem.viewItem.webView.rememberLoadingProgress()) {
      0f, 1f -> {}
      else -> {
        LinearProgressIndicator(
          progress = loadingProgress,
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .align(Alignment.BottomCenter),
          color = MaterialTheme.colorScheme.primary,
        )
      }
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
  val scope = rememberCoroutineScope()
  var showSearchView by LocalShowSearchView.current
  val searchHint = BrowserI18nResource.browser_search_hint()
  val focusManager = LocalFocusManager.current
  if (showSearchView) {
    val dwebLink = viewModel.dwebLinkSearch.value
    val inputText = if (dwebLink.trim().isEmpty() || dwebLink == ConstUrl.BLANK.url) {
      viewModel.currentTab?.contentWebItem?.value?.viewItem?.webView?.getUrl() ?: ""
    } else dwebLink
    val showText = if (inputText.isSystemUrl() || inputText == searchHint) {
      ""
    } else {
      inputText
    }

    val inputTextState = LocalInputText.current

    Box(modifier = Modifier.fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .clickableWithNoEffect {
        focusManager.clearFocus()
        showSearchView = false
      }
    ) {
      SearchView(
        text = showText,
        modifier = modifier,
        homePreview = { _ -> HomePage() },
        onClose = {
          showSearchView = false
        },
        onSearch = { url -> // 第一个是搜索关键字，第二个是搜索地址
          scope.launch {
            showSearchView = false
            viewModel.saveLastKeyword(inputTextState, url)
            viewModel.searchWebView(url)
          }
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