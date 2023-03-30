package info.bagen.rust.plaoc.ui.browser

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import info.bagen.rust.plaoc.R
import info.bagen.rust.plaoc.ui.theme.Blue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class, ExperimentalComposeUiApi::class)
@Composable
fun BrowserView(viewModel: BrowserViewModel) {
  val pagerStateSearch = rememberPagerState()
  val pagerStateWebView = rememberPagerState()
  val scope = rememberCoroutineScope()

  Column(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier.weight(1f)) {
      BrowserViewContent(viewModel, pagerStateSearch, pagerStateWebView)
    }
    BrowserViewSearch(viewModel, pagerStateSearch)
    BrowserViewBottomBar(viewModel) {
      scope.launch {
        pagerStateSearch.animateScrollToPage(pagerStateSearch.pageCount)
        pagerStateWebView.animateScrollToPage(pagerStateWebView.pageCount)
      }
    }
  }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun BrowserViewContent(
  viewModel: BrowserViewModel, pagerStateSearch: PagerState, pagerState: PagerState
) {
  LaunchedEffect(pagerStateSearch) {
    snapshotFlow { pagerStateSearch.currentPageOffset }.collect { currentPageOffset ->
      pagerState.scrollToPage(pagerStateSearch.currentPage, currentPageOffset)
    }
  }
  // 创建一个不可滑动的 HorizontalPager , 然后由底下的 Search 来控制滑动效果
  HorizontalPager(
    count = viewModel.uiState.browserViewList.size, state = pagerState, userScrollEnabled = false
  ) { currentPage ->
    when (val item = viewModel.uiState.browserViewList[currentPage]) {
      is BrowserMainView -> BrowserViewContentMain(viewModel, item)
      is BrowserWebView -> BrowserViewContentWeb(viewModel, item)
    }
    viewModel.handleIntent(BrowserIntent.UpdateCurrentWebView(pagerStateSearch.currentPage))
  }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun BrowserViewSearch(viewModel: BrowserViewModel, pagerState: PagerState) {
  HorizontalPager(
    state = pagerState,
    count = viewModel.uiState.browserViewList.size,
    contentPadding = PaddingValues(horizontal = 20.dp),
    modifier = Modifier.background(Color.LightGray)
  ) { currentPage ->
    if (currentPage == pagerState.pageCount - 1) viewModel.handleIntent(BrowserIntent.ShowMainView)
    when (val item = viewModel.uiState.browserViewList[currentPage]) {
      is BrowserWebView -> BrowserViewSearchWeb(viewModel, item)
      is BrowserMainView -> BrowserViewSearchMain(viewModel, item, pagerState)
    }
  }
}

@Composable
private fun BrowserViewBottomBar(viewModel: BrowserViewModel, onHome: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(40.dp)
  ) {
    val navigator = viewModel.uiState.currentBrowserWebView.value?.navigator
    NavigatorButton(
      resId = R.drawable.ic_main_back,
      resName = R.string.browser_nav_back,
      show = navigator?.canGoBack ?: false
    ) { navigator?.navigateBack() }
    NavigatorButton(
      resId = R.drawable.ic_main_forward,
      resName = R.string.browser_nav_forward,
      show = navigator?.canGoForward ?: false
    ) { navigator?.navigateForward() }
    NavigatorButton(resId = R.drawable.ic_main_book,
      resName = R.string.browser_nav_book,
      show = navigator?.let { true } ?: false
    ) { /* TODO 将当前的地址添加到书签 */ }
    NavigatorButton(
      resId = R.drawable.ic_main_option, resName = R.string.browser_nav_option, show = true
    ) { /* TODO 打开弹窗，里面有历史浏览记录和书签列表 */ }
    NavigatorButton(resId = R.drawable.ic_main_home,
      resName = R.string.browser_nav_home,
      show = navigator?.let { true } ?: false
    ) { onHome() }
  }
}

@Composable
private fun RowScope.NavigatorButton(
  @DrawableRes resId: Int, @StringRes resName: Int, show: Boolean, onClick: () -> Unit
) {
  Box(modifier = Modifier
    .weight(1f)
    .padding(horizontal = 2.dp)
    .clickable {
      if (show) {
        onClick()
      }
    }) {
    Column(modifier = Modifier.align(Alignment.Center)) {
      Icon(
        modifier = Modifier.padding(5.dp),
        bitmap = ImageBitmap.imageResource(id = resId),
        contentDescription = stringResource(id = resName),
        tint = if (show) Blue else Color.LightGray
      )
    }
  }
}

@Composable
private fun BrowserViewContentMain(viewModel: BrowserViewModel, browserMainView: BrowserMainView) {
  Box(modifier = Modifier.fillMaxSize()) {
    BrowserMainView()
  }
}

@Composable
private fun BrowserViewContentWeb(viewModel: BrowserViewModel, browserWebView: BrowserWebView) {
  BackHandler {
    if (browserWebView.navigator.canGoBack) {
      viewModel.handleIntent(BrowserIntent.WebViewGoBack)
    }
  }
  key(browserWebView.webViewId) {
    Box(modifier = Modifier.fillMaxSize()) {
      WebView(
        state = browserWebView.state,
        navigator = browserWebView.navigator,
        factory = {
          browserWebView.webView.parent?.let { (it as ViewGroup).removeAllViews() }
          browserWebView.webView
        },
      )
    }
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
private fun SearchBox(
  baseView: BrowserBaseView, showCamera: Boolean = false, search: (String) -> Unit
) {
  val focusRequester = remember { FocusRequester() }
  Box(
    modifier = Modifier
      .padding(horizontal = 5.dp, vertical = 10.dp)
      .fillMaxWidth()
      .height(40.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(Color.White)
  ) {
    val inputText = when (baseView) {
      is BrowserWebView -> {
        ShowLinearProgressIndicator(baseView)
        mutableStateOf(baseView.state.lastLoadedUrl ?: "")
      }
      else -> mutableStateOf("")
    }
    SearchTextField(inputText, showCamera, baseView.focus, focusRequester, search)
    SearchText(inputText, showCamera, baseView.focus, focusRequester)
  }
}

/**
 * 用于显示 WebView 加载进度
 */
@Composable
private fun BoxScope.ShowLinearProgressIndicator(browserWebView: BrowserWebView?) {
  browserWebView?.let {
    when (val loadingState = it.state.loadingState) {
      is LoadingState.Loading -> {
        LinearProgressIndicator(
          progress = loadingState.progress,
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .align(Alignment.BottomCenter),
          color = Color.Blue
        )
      }
      else -> {}
    }
  }
}

@Composable
private fun BoxScope.SearchText(
  inputText: MutableState<String>,
  showCamera: Boolean = false,
  focus: MutableState<Boolean>,
  focusRequester: FocusRequester
) {
  val scope = rememberCoroutineScope()
  AnimatedVisibility(
    visible = !focus.value,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = Modifier
      .align(Alignment.Center)
      .clickable {
        focus.value = true
        scope.launch {
          delay(1000)
          focusRequester.requestFocus()
        }
      }
      .padding(horizontal = 8.dp)
  ) {
    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = null,
        tint = MaterialTheme.colors.onSecondary
      )

      Box(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 4.dp)
      ) {
        Uri.parse(inputText.value)?.host?.let { host ->
          Text(text = host, modifier = Modifier.align(Alignment.Center))
        } ?: Text(
          text = stringResource(id = R.string.browser_search_hint),
          modifier = Modifier.align(Alignment.CenterStart)
        )
      }

      if (showCamera) {
        Icon(
          imageVector = ImageVector.vectorResource(id = R.drawable.ic_photo_camera_24),
          contentDescription = null,
          tint = MaterialTheme.colors.onSecondary,
        )
      }
    }
  }
}

@Composable
private fun BoxScope.SearchTextField(
  inputText: MutableState<String>,
  showCamera: Boolean = false,
  focus: MutableState<Boolean>,
  focusRequester: FocusRequester,
  search: (String) -> Unit
) {
  val focusManager = LocalFocusManager.current

  AnimatedVisibility(
    visible = focus.value,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = Modifier
      .align(Alignment.Center)
      .padding(horizontal = 8.dp)
  ) {
    BasicTextField(
      value = inputText.value,
      onValueChange = { inputText.value = it },
      readOnly = false,
      enabled = true,
      modifier = Modifier
        .fillMaxSize()
        .focusRequester(focusRequester)
        .focusable(),
      singleLine = true,
      textStyle = TextStyle.Default.copy(color = MaterialTheme.colors.onPrimary),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = {
        if (inputText.value.isEmpty()) return@KeyboardActions
        focusManager.clearFocus() // 取消聚焦，就会间接的隐藏键盘
        focusManager.moveFocus(FocusDirection.Next)
        focus.value = false
        val requestUrl = Uri.parse(inputText.value)?.let { uri ->
          if ((uri.scheme == "http" || uri.scheme == "https") && uri.host?.isNotEmpty() == true) {
            inputText.value
          } else null
        } ?: "https://cn.bing.com/search?q=${inputText.value}"
        search(requestUrl)
      })
    ) { innerTextField ->
      Box {
        Surface(
          modifier = Modifier.align(Alignment.Center),
          shape = RoundedCornerShape(16.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
            ) {
              if (inputText.value.isEmpty()) {
                Text(
                  text = stringResource(id = R.string.browser_search_hint),
                  color = MaterialTheme.colors.onSecondary,
                  modifier = Modifier.align(Alignment.CenterStart)
                )
              }
              innerTextField()
            }

            if (inputText.value.isNotEmpty()) {
              Icon(imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = MaterialTheme.colors.onSecondary,
                modifier = Modifier.clickable { inputText.value = "" })
            } else if (showCamera) {
              Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_photo_camera_24),
                contentDescription = null,
                tint = MaterialTheme.colors.onSecondary,
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun BrowserViewSearchMain(
  viewModel: BrowserViewModel, browserMainView: BrowserMainView, pagerState: PagerState
) {
  SearchBox(browserMainView, showCamera = true) { url ->
    viewModel.handleIntent(BrowserIntent.AddNewWebView(url))
  }
  // TODO 这边考虑加一层遮罩，颜色随着滑动而显示
  if (!browserMainView.show.value) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.White.copy(pagerState.currentPageOffset))
    )
  }
}

@Composable
private fun BrowserViewSearchWeb(viewModel: BrowserViewModel, browserWebView: BrowserWebView) {
  SearchBox(browserWebView, showCamera = false) { url ->
    viewModel.handleIntent(BrowserIntent.SearchWebView(url))
  }
}