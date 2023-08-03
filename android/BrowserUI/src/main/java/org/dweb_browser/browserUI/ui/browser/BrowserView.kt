package org.dweb_browser.browserUI.ui.browser

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import org.dweb_browser.browserUI.R
import org.dweb_browser.browserUI.ui.entity.BrowserBaseView
import org.dweb_browser.browserUI.ui.entity.BrowserWebView
import org.dweb_browser.browserUI.ui.qrcode.QRCodeScanView
import org.dweb_browser.browserUI.ui.view.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect
import org.dweb_browser.browserUI.ui.loading.LoadingView
import org.dweb_browser.browserUI.ui.view.PrivacyView

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BrowserView(viewModel: BrowserViewModel) {
  val scope = rememberCoroutineScope()
  val activity = LocalContext.current.findActivity()

  BackHandler {
    val watcher = viewModel.uiState.currentBrowserBaseView.value.closeWatcher;
    val bottomState = viewModel.uiState.bottomSheetScaffoldState.bottomSheetState;
    if (bottomState.targetValue != SheetValue.Hidden) {
      scope.launch {
        bottomState.hide()
      }
    } else if (watcher.canClose) {
      scope.launch {
        watcher.close()
      }
    } else {
      val browserWebView = viewModel.uiState.currentBrowserBaseView.value
      val navigator = browserWebView.viewItem.navigator
      if (navigator.canGoBack) {
        navigator.navigateBack()
      } else {
        activity.moveTaskToBack(false) // 将界面转移到后台
      }
    }
  }

  BottomSheetScaffold(modifier = Modifier.navigationBarsPadding(),
    scaffoldState = viewModel.uiState.bottomSheetScaffoldState,
    sheetPeekHeight = 540.dp, //LocalConfiguration.current.screenHeightDp.dp / 2,
    sheetContainerColor = MaterialTheme.colorScheme.background,
    sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    sheetContent = {
      Box(modifier = Modifier.navigationBarsPadding()) {
        BrowserPopView(viewModel)       // 用于处理弹出框
      }
    }
  ) {
    Box(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      BrowserViewContent(viewModel)   // 中间主体部分
      // BrowserSearchPreview(viewModel) // 地址栏输入内容后，上面显示的书签、历史和相应搜索引擎
      BrowserViewBottomBar(viewModel) // 工具栏，包括搜索框和导航栏
      // BrowserPopView(viewModel)    // 用于处理弹出框
      BrowserMultiPopupView(viewModel)// 用于显示多界面
      BrowserSearchView(viewModel)
    }
    BrowserMaskView(viewModel) {
      scope.launch {
        viewModel.uiState.bottomSheetScaffoldState.bottomSheetState.hide()
      }
    }

    // 增加扫码的界面
    QRCodeScanView(
      qrCodeScanState = viewModel.uiState.qrCodeScanState,
      onDataCallback = { data ->
        if (data.startsWith("http://") || data.startsWith("https://")) {
          viewModel.handleIntent(BrowserIntent.SearchWebView(data))
        } else {
          viewModel.handleIntent(BrowserIntent.ShowSnackbarMessage("扫码结果：$data"))
        }
      })

    val showLoading = remember { mutableStateOf(false) }
    PrivacyView(url = viewModel.uiState.privacyState, showLoading = showLoading)
    LoadingView(showLoading = showLoading)
  }
  LaunchedEffect(Unit) { // TODO 这个是因为华为鸿蒙系统，运行后，半屏显示了Sheet，这边强制隐藏下
    scope.launch {
      delay(15)
      viewModel.uiState.bottomSheetScaffoldState.bottomSheetState.hide()
    }
  }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserMaskView(viewModel: BrowserViewModel, onClick: () -> Unit) {
  // 如果显示了sheet，这边做一层遮罩
  if (viewModel.uiState.bottomSheetScaffoldState.bottomSheetState.targetValue != SheetValue.Hidden) {
    Box(modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.onSurface.copy(0.2f))
      .clickableWithNoEffect { onClick() }
    )
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
  Box(
    modifier = Modifier
      .fillMaxSize()
      .clickableWithNoEffect { localFocusManager.clearFocus() }
  ) {
    // 创建一个不可滑动的 HorizontalPager , 然后由底下的 Search 来控制滑动效果
    /*when (val item = viewModel.uiState.browserViewList[currentPage]) {
        is BrowserMainView -> BrowserViewContentMain(viewModel, item)
        is BrowserWebView -> BrowserViewContentWeb(viewModel, item)
      }*/
    HorizontalPager(
      modifier = Modifier,
      state = pagerStateContent,
      pageSpacing = 0.dp,
      userScrollEnabled = false,
      reverseLayout = false,
      contentPadding = PaddingValues(0.dp),
      beyondBoundsPageCount = 5,

      pageContent = { currentPage ->
        BrowserViewContentWeb(viewModel, viewModel.uiState.browserViewList[currentPage])
        /*when (val item = viewModel.uiState.browserViewList[currentPage]) {
          is BrowserMainView -> BrowserViewContentMain(viewModel, item)
          is BrowserWebView -> BrowserViewContentWeb(viewModel, item)
        }*/
      }
    )
  }
}

@Composable
fun ColumnScope.MiniTitle(viewModel: BrowserViewModel) {
  val browserBaseView = viewModel.uiState.currentBrowserBaseView.value
  val inputText = parseInputText(browserBaseView.viewItem.state.lastLoadedUrl ?: "")

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
  HorizontalPager(
    modifier = Modifier,
    state = pagerStateNavigator,
    pageSpacing = 0.dp,
    userScrollEnabled = true,
    reverseLayout = false,
    contentPadding = PaddingValues(horizontal = dimenHorizontalPagerHorizontal),
    beyondBoundsPageCount = 0,
    pageContent = { currentPage ->
      SearchBox(viewModel.uiState.browserViewList[currentPage])
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserViewNavigatorBar(viewModel: BrowserViewModel) {
  val scope = rememberCoroutineScope()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(dimenNavigationHeight)
  ) {
    val navigator = viewModel.uiState.currentBrowserBaseView.value.viewItem.navigator
    NavigatorButton(
      resId = R.drawable.ic_main_back,
      resName = R.string.browser_nav_back,
      show = navigator.canGoBack
    ) { navigator.navigateBack() }
    NavigatorButton(
      resId = R.drawable.ic_main_forward,
      resName = R.string.browser_nav_forward,
      show = navigator.canGoForward ?: false
    ) { navigator.navigateForward() }
    NavigatorButton(
      resId = if (navigator.canGoBack) R.drawable.ic_main_add else R.drawable.ic_main_qrcode_scan,
      resName = if (navigator.canGoBack) R.string.browser_nav_add else R.string.browser_nav_scan,
      show = true
    ) {
      if (navigator.canGoBack) {
        viewModel.handleIntent(BrowserIntent.AddNewMainView)
      } else {
        scope.launch { viewModel.uiState.qrCodeScanState.show() }
      }
    }
    NavigatorButton(
      resId = R.drawable.ic_main_multi, resName = R.string.browser_nav_multi, show = true
    ) {
      viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(true))
    }
    NavigatorButton(
      resId = R.drawable.ic_main_option, resName = R.string.browser_nav_option, show = true
    ) {
      scope.launch {
        viewModel.uiState.bottomSheetScaffoldState.bottomSheetState.show()
      }
    }
  }
}

@Composable
private fun RowScope.NavigatorButton(
  @DrawableRes resId: Int, @StringRes resName: Int, show: Boolean, onClick: () -> Unit
) {
  Box(modifier = Modifier
    .weight(1f)
    .fillMaxHeight()
    .padding(horizontal = 2.dp)
    .clickable(enabled = show) { onClick() }) {
    Column(modifier = Modifier.align(Alignment.Center)) {
      Icon(
        modifier = Modifier.size(28.dp),
        imageVector = ImageVector.vectorResource(id = resId),//ImageBitmap.imageResource(id = resId),
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
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = if (viewModel.uiState.showBottomBar.currentState) dimenBottomHeight else dimenHorizontalPagerHorizontal)
    ) {
      BrowserWebView(viewModel = viewModel, browserWebView = browserWebView)
    }
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
private fun SearchBox(baseView: BrowserBaseView) {
  var showSearchView by LocalShowSearchView.current;
  Box(modifier = Modifier
    .padding(horizontal = dimenSearchHorizontalAlign, vertical = dimenSearchVerticalAlign)
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
    val search =
      if (inputText.value.isEmpty() || inputText.value.isSystemUrl()) {
        Triple(
          stringResource(id = R.string.browser_search_hint),
          TextAlign.Start,
          Icons.Default.Search
        )
      } else {
        Triple(
          parseInputText(inputText.value),
          TextAlign.Center,
          Icons.Default.FormatSize
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
    val inputText =
      viewModel.uiState.currentBrowserBaseView.value.viewItem.state.lastLoadedUrl ?: ""
    val text = if (inputText.isSystemUrl() ||
      inputText == stringResource(id = R.string.browser_search_hint)
    ) {
      ""
    } else {
      inputText
    }

    val inputTextState = LocalInputText.current;

    SearchView(
      text = text,
      homePreview = { onMove ->
        HomeWebviewPage(viewModel, onMove)
      },
      onClose = {
        showSearchView = false
      },
      onSearch = { url -> // 第一个是搜索关键字，第二个是搜索地址
        showSearchView = false
        BrowserViewModelHelper.saveLastKeyword(inputTextState, url)
        viewModel.handleIntent(BrowserIntent.SearchWebView(url))
      })
  }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
internal fun HomeWebviewPage(viewModel: BrowserViewModel, onClickOrMove: (Boolean) -> Unit) {
  val webView = viewModel.getNewTabBrowserView()
  val background = MaterialTheme.colorScheme.background
  val isDark = isSystemInDarkTheme()
  var isRemove = false
  WebView(
    state = webView.viewItem.state,
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
    }
  )
}