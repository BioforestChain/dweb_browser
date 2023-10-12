package org.dweb_browser.browser.web.ui.browser

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.R
import org.dweb_browser.browser.web.ui.browser.bottomsheet.LocalModalBottomSheet
import org.dweb_browser.browser.web.ui.browser.bottomsheet.ModalBottomModel
import org.dweb_browser.browser.web.ui.browser.bottomsheet.SheetState
import org.dweb_browser.browser.web.ui.browser.model.BrowserBaseView
import org.dweb_browser.browser.web.ui.browser.model.BrowserIntent
import org.dweb_browser.browser.web.ui.browser.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.browser.model.BrowserViewModelHelper
import org.dweb_browser.browser.web.ui.browser.model.BrowserWebView
import org.dweb_browser.browser.web.ui.browser.model.LocalInputText
import org.dweb_browser.browser.web.ui.browser.model.LocalShowIme
import org.dweb_browser.browser.web.ui.browser.model.LocalShowSearchView
import org.dweb_browser.browser.web.ui.browser.model.LocalWebViewInitialScale
import org.dweb_browser.browser.web.ui.browser.model.isSystemUrl
import org.dweb_browser.browser.web.ui.browser.model.parseInputText
import org.dweb_browser.browser.web.ui.browser.search.SearchView
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.sys.window.core.WindowRenderScope

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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BrowserViewForWindow(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowRenderScope
) {
  val scope = rememberCoroutineScope()
  val initialScale =
    (LocalDensity.current.density * windowRenderScope.scale * 100).toInt() // 用于WebView缩放，避免点击后位置不对

  CompositionLocalProvider(
    LocalModalBottomSheet provides ModalBottomModel(remember { mutableStateOf(SheetState.PartiallyExpanded) })
  ) {
    val bottomSheetModel = LocalModalBottomSheet.current
    BackHandler {
      val watcher = viewModel.uiState.currentBrowserBaseView.value?.closeWatcher;
      if (bottomSheetModel.state.value != SheetState.Hidden) {
        scope.launch {
          bottomSheetModel.hide()
        }
      } else if (watcher?.canClose == true) {
        scope.launch {
          watcher.close()
        }
      } else {
        viewModel.uiState.currentBrowserBaseView.value?.viewItem?.navigator?.let { navigator ->
          if (navigator.canGoBack) {
            navigator.navigateBack()
          }
        }
      }
    }

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
      CompositionLocalProvider(
        LocalWebViewInitialScale provides initialScale
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = dimenBottomHeight * windowRenderScope.scale)
        ) {
          BrowserViewContent(viewModel)   // 中间主体部分
        }
      }
      Box(modifier = with(windowRenderScope) {
        Modifier
          .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
          .scale(scale)
      }) {
        // BrowserSearchPreview(viewModel) // 地址栏输入内容后，上面显示的书签、历史和相应搜索引擎
        BrowserViewBottomBar(viewModel) // 工具栏，包括搜索框和导航栏
        // BrowserPopView(viewModel)    // 用于处理弹出框
        BrowserMultiPopupView(viewModel)// 用于显示多界面
        BrowserSearchView(viewModel)
        BrowserBottomSheet(viewModel)

        // 增加扫码的界面 // 暂时屏蔽qrCode
        /*QRCodeScanView(qrCodeScanState = viewModel.uiState.qrCodeScanState,
          onDataCallback = { data ->
            if (data.isUrlOrHost() || data.startsWith("dweb:")) {
              viewModel.handleIntent(BrowserIntent.SearchWebView(data))
            } else {
              viewModel.handleIntent(BrowserIntent.ShowSnackbarMessage("扫码结果：$data"))
            }
          })*/
      }
    }
  }
}

@Composable
private fun BrowserMaskView(viewModel: BrowserViewModel, onClick: () -> Unit) {
  // 如果显示了sheet，这边做一层遮罩
  val bottomSheetModel = LocalModalBottomSheet.current
  if (bottomSheetModel.state.value != SheetState.Hidden) {
    Box(modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.onSurface.copy(0.2f))
      .clickableWithNoEffect { onClick() })
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserViewContent(viewModel: BrowserViewModel) {
  val localFocusManager = LocalFocusManager.current
  val pagerStateNavigator = rememberPagerState {
    viewModel.uiState.browserViewList.size
  }
  val pagerStateContent = rememberPagerState {
    viewModel.uiState.browserViewList.size
  }
  viewModel.uiState.pagerStateNavigator.value = pagerStateNavigator
  viewModel.uiState.pagerStateContent.value = pagerStateContent
  LaunchedEffect(pagerStateNavigator.currentPageOffsetFraction) {
    pagerStateContent.scrollToPage(
      pagerStateNavigator.currentPage, pagerStateNavigator.currentPageOffsetFraction
    )
  }
  LaunchedEffect(pagerStateContent.currentPage) {
    viewModel.handleIntent(BrowserIntent.UpdateCurrentBaseView(pagerStateContent.currentPage))
  }

  Box(modifier = Modifier
    .fillMaxSize()
    .clickableWithNoEffect { localFocusManager.clearFocus() }) {
    // 创建一个不可滑动的 HorizontalPager , 然后由底下的 Search 来控制滑动效果
    /*when (val item = viewModel.uiState.browserViewList[currentPage]) {
        is BrowserMainView -> BrowserViewContentMain(viewModel, item)
        is BrowserWebView -> BrowserViewContentWeb(viewModel, item)
      }*/
    HorizontalPager(modifier = Modifier,
      state = pagerStateContent,
      pageSpacing = 0.dp,
      userScrollEnabled = false,
      reverseLayout = false,
      contentPadding = PaddingValues(0.dp),
      beyondBoundsPageCount = 5,

      pageContent = { currentPage ->
        BrowserViewContentWeb(viewModel, viewModel.uiState.browserViewList[currentPage])/*when (val item = viewModel.uiState.browserViewList[currentPage]) {
          is BrowserMainView -> BrowserViewContentMain(viewModel, item)
          is BrowserWebView -> BrowserViewContentWeb(viewModel, item)
        }*/
      })
  }
}

@Composable
fun ColumnScope.MiniTitle(viewModel: BrowserViewModel) {
  val browserBaseView = viewModel.uiState.currentBrowserBaseView.value
  val inputText = parseInputText(browserBaseView?.viewItem?.state?.lastLoadedUrl ?: "")

  Text(
    text = inputText, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally)
  )
}

@Composable
private fun BoxScope.BrowserViewBottomBar(viewModel: BrowserViewModel) {
  Box(modifier = Modifier.align(Alignment.BottomCenter)) {
    Column(modifier = Modifier
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
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserViewSearch(viewModel: BrowserViewModel) {
  val pagerStateNavigator = viewModel.uiState.pagerStateNavigator.value ?: return
  val localShowIme = LocalShowIme.current

  LaunchedEffect(pagerStateNavigator.settledPage) { // 为了修复隐藏搜索框后，重新加载时重新显示的问题，会显示第一页
    delay(100)
    pagerStateNavigator.scrollToPage(pagerStateNavigator.settledPage)
  }
  val localFocus = LocalFocusManager.current
  LaunchedEffect(Unit) {
    if (!localShowIme.value && !viewModel.uiState.showSearchEngine.targetState) {
      localFocus.clearFocus()
    }
  }

  // 增加判断是否有传入需要检索的内容，如果有，就进行显示搜索界面
  val showSearchView = LocalShowSearchView.current
  LaunchedEffect(showSearchView) {
    snapshotFlow { viewModel.dwebLinkSearch.value }.collect {
      showSearchView.value = it.isNotEmpty()
    }
  }

  HorizontalPager(modifier = Modifier,
    state = pagerStateNavigator,
    pageSpacing = 0.dp,
    userScrollEnabled = true,
    reverseLayout = false,
    contentPadding = PaddingValues(horizontal = dimenHorizontalPagerHorizontal),
    beyondBoundsPageCount = 0,
    pageContent = { currentPage ->
      SearchBox(viewModel.uiState.browserViewList[currentPage])
    })
}

@Composable
private fun BrowserViewNavigatorBar(viewModel: BrowserViewModel) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val bottomSheetModel = LocalModalBottomSheet.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(dimenNavigationHeight)
  ) {
    val navigator = viewModel.uiState.currentBrowserBaseView.value?.viewItem?.navigator ?: return
    // 屏蔽 goBack 和 goForward，功能转移到窗口的导航栏实现
    /*NavigatorButton(
      imageVector = Icons.Rounded.ArrowBack, // R.drawable.ic_main_back,
      resName = R.string.browser_nav_back,
      show = navigator.canGoBack
    ) { navigator.navigateBack() }
    NavigatorButton(
      imageVector = Icons.Rounded.ArrowForward, // R.drawable.ic_main_forward,
      resName = R.string.browser_nav_forward,
      show = navigator.canGoForward ?: false
    ) { navigator.navigateForward() }*/
    NavigatorButton(
      imageVector = Icons.Rounded.AddHome,
      resName = R.string.browser_nav_addhome,
      show = viewModel.uiState.currentBrowserBaseView.value?.viewItem?.state?.lastLoadedUrl != null
    ) {
      scope.launch { viewModel.addUrlToDesktop(context) }
    }
    NavigatorButton(
      imageVector = if (navigator.canGoBack) Icons.Rounded.Add else Icons.Rounded.QrCodeScanner,
      // resId = if (navigator.canGoBack) R.drawable.ic_main_add else R.drawable.ic_main_qrcode_scan,
      resName = if (navigator.canGoBack) R.string.browser_nav_add else R.string.browser_nav_scan,
      show = true
    ) {
      if (navigator.canGoBack) {
        viewModel.handleIntent(BrowserIntent.AddNewMainView())
      } else {
        // scope.launch { viewModel.uiState.qrCodeScanState.show() } // 暂时屏蔽qrCode
      }
    }
    NavigatorButton(
      imageVector = getMultiImageVector(viewModel.uiState.browserViewList.size), // resId = R.drawable.ic_main_multi,
      resName = R.string.browser_nav_multi, show = true
    ) {
      viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(true))
    }
    NavigatorButton(
      imageVector = Icons.Rounded.Menu, // resId = R.drawable.ic_main_option,
      resName = R.string.browser_nav_option, show = true
    ) {
      scope.launch {
        bottomSheetModel.show()
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
  imageVector: ImageVector, @StringRes resName: Int, show: Boolean, onClick: () -> Unit
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
        contentDescription = stringResource(id = resName),
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
private fun BrowserViewContentWeb(viewModel: BrowserViewModel, browserWebView: BrowserWebView) {
  key(browserWebView.viewItem.webviewId) {
    BrowserWebView(viewModel = viewModel, browserWebView = browserWebView)
    /*Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = if (viewModel.uiState.showBottomBar.currentState) dimenBottomHeight else dimenHorizontalPagerHorizontal)
    ) {
      BrowserWebView(viewModel = viewModel, browserWebView = browserWebView)
    }*/
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
private fun SearchBox(baseView: BrowserBaseView) {
  var showSearchView by LocalShowSearchView.current

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
    .clickable {
      showSearchView = true;
    }) {
    val inputText = when (baseView) {
      is BrowserWebView -> {
        ShowLinearProgressIndicator(baseView)
        mutableStateOf(baseView.viewItem.state.lastLoadedUrl ?: "")
      }

      else -> mutableStateOf("")
    }
    val search = if (inputText.value.isEmpty() || inputText.value.isSystemUrl()) {
      Triple(
        stringResource(id = R.string.browser_search_hint), TextAlign.Start, Icons.Default.Search
      )
    } else {
      Triple(
        parseInputText(inputText.value), TextAlign.Center, Icons.Default.FormatSize
      )
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp)
        .align(Alignment.Center),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(search.third, contentDescription = "Search")
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = search.first,
        textAlign = search.second,
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
private fun BoxScope.ShowLinearProgressIndicator(browserWebView: BrowserWebView?) {
  browserWebView?.let {
    when (val loadingState = it.viewItem.state.loadingState) {
      is LoadingState.Loading -> {
        LinearProgressIndicator(
          progress = loadingState.progress,
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .align(Alignment.BottomCenter),
          color = MaterialTheme.colorScheme.primary
        )
      }

      else -> {}
    }
  }
}

/**
 * 提供给外部调用的  搜索界面，可以含有BrowserViewModel
 */
@Composable
fun BrowserSearchView(viewModel: BrowserViewModel) {
  var showSearchView by LocalShowSearchView.current
  if (showSearchView) {
    val inputText = viewModel.dwebLinkSearch.value.ifEmpty {
      viewModel.uiState.currentBrowserBaseView.value?.viewItem?.state?.lastLoadedUrl ?: ""
    }
    val text =
      if (inputText.isSystemUrl() || inputText == stringResource(id = R.string.browser_search_hint)) {
        ""
      } else {
        inputText
      }

    val inputTextState = LocalInputText.current

    SearchView(text = text, homePreview = { onMove ->
      HomeWebviewPage(viewModel, onMove)
    }, onClose = {
      showSearchView = false
    }, onSearch = { url -> // 第一个是搜索关键字，第二个是搜索地址
      showSearchView = false
      BrowserViewModelHelper.saveLastKeyword(inputTextState, url)
      viewModel.handleIntent(BrowserIntent.SearchWebView(url))
    })
  }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
internal fun HomeWebviewPage(viewModel: BrowserViewModel, onClickOrMove: (Boolean) -> Unit) {
  val webView = viewModel.searchBackBrowserView // getNewTabBrowserView()
  val background = MaterialTheme.colorScheme.background
  val isDark = isSystemInDarkTheme()
  var isRemove = false
  WebView(state = webView.viewItem.state,
    modifier = Modifier
      .fillMaxSize()
      .background(background),
    navigator = webView.viewItem.navigator,
    onCreated = {
      it.setDarkMode(isDark, background) // 为了保证浏览器背景色和系统主题一致
      it.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_MOVE) {
          isRemove = true
        } else if (event.action == MotionEvent.ACTION_UP) {
          onClickOrMove(isRemove)
        }
        false
      }
    },
    factory = {
      webView.viewItem.webView.parent?.let { (it as ViewGroup).removeAllViews() }
      webView.viewItem.webView
    })
}