package info.bagen.rust.plaoc.ui.browser

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import info.bagen.rust.plaoc.R
import info.bagen.rust.plaoc.ui.theme.Blue
import kotlinx.coroutines.launch

private val dimenTextFieldFontSize = 16.sp
private val dimenSearchHorizontalAlign = 5.dp
private val dimenSearchVerticalAlign = 10.dp
private val dimenSearchRoundedCornerShape = 8.dp
private val dimenShadowElevation = 4.dp
private val dimenHorizontalPagerHorizontal = 20.dp
private val dimenBottomHeight = 100.dp
private val dimenSearchHeight = 40.dp

private val bottomEnterAnimator = slideInVertically(
  animationSpec = tween(100),//动画时长1s
  initialOffsetY = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  }
) + fadeIn()
private val bottomExitAnimator = slideOutVertically(
  animationSpec = tween(100),//动画时长1s
  targetOffsetY = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  }
) + fadeOut()

@OptIn(ExperimentalPagerApi::class)
@Composable
fun BrowserView(viewModel: BrowserViewModel) {
  val pagerStateSearch = rememberPagerState()
  val pagerStateWebView = rememberPagerState()
  val localFocusManager = LocalFocusManager.current

  Column(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier
      .weight(1f)
      .clickable(
        indication = null,
        onClick = { localFocusManager.clearFocus() },
        interactionSource = remember { MutableInteractionSource() }
      )) {
      BrowserViewContent(viewModel, pagerStateSearch, pagerStateWebView)
    }
    BrowserViewBottomBar(viewModel, pagerStateSearch, pagerStateWebView)
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
      is BrowserMainView -> BrowserViewContentMain(viewModel, item, pagerStateSearch)
      is BrowserWebView -> BrowserViewContentWeb(viewModel, item)
    }
    viewModel.handleIntent(BrowserIntent.UpdateCurrentWebView(pagerStateSearch.currentPage))
  }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun BrowserViewBottomBar(
  viewModel: BrowserViewModel, pagerStateSearch: PagerState, pagerStateWebView: PagerState
) {
  val coroutineScope = rememberCoroutineScope()
  val browserBaseView = viewModel.uiState.currentBrowserBaseView.value

  val inputText = when (browserBaseView) {
    is BrowserWebView -> {
      parseInputText(browserBaseView.state.lastLoadedUrl ?: "")
        ?: stringResource(id = R.string.browser_search_hint)
    }
    else -> stringResource(id = R.string.browser_search_hint)
  }

  Box {
    Column(modifier = Modifier
      .fillMaxWidth()
      .height(20.dp).align(Alignment.BottomCenter)
      .clickable { browserBaseView.showBottomBar.value = true }) {
      Text(
        text = inputText,
        fontSize = 12.sp,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      )
    }

    AnimatedVisibility(
      visible = browserBaseView.showBottomBar.value,
      enter = bottomEnterAnimator,
      exit = bottomExitAnimator
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        BrowserViewSearch(viewModel, pagerStateSearch)
        BrowserViewNavigatorBar(viewModel) {
          coroutineScope.launch {
            pagerStateSearch.animateScrollToPage(pagerStateSearch.pageCount)
            pagerStateWebView.animateScrollToPage(pagerStateWebView.pageCount)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun BrowserViewSearch(viewModel: BrowserViewModel, pagerState: PagerState) {
  HorizontalPager(
    state = pagerState,
    count = viewModel.uiState.browserViewList.size,
    contentPadding = PaddingValues(horizontal = dimenHorizontalPagerHorizontal),
    modifier = Modifier.background(MaterialTheme.colors.primaryVariant)
  ) { currentPage ->
    if (currentPage == pagerState.pageCount - 1) viewModel.handleIntent(BrowserIntent.ShowMainView)
    when (val item = viewModel.uiState.browserViewList[currentPage]) {
      is BrowserWebView -> BrowserViewSearchWeb(viewModel, item)
      is BrowserMainView -> BrowserViewSearchMain(viewModel, item, pagerState)
    }
  }
}

@Composable
private fun BrowserViewNavigatorBar(viewModel: BrowserViewModel, onHome: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(dimenSearchHeight)
      .background(MaterialTheme.colors.primaryVariant)
  ) {
    val navigator = when (val item = viewModel.uiState.currentBrowserBaseView.value) {
      is BrowserWebView -> item.navigator
      else -> null
    }
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
        modifier = Modifier.padding(dimenSearchHorizontalAlign),
        bitmap = ImageBitmap.imageResource(id = resId),
        contentDescription = stringResource(id = resName),
        tint = if (show) Blue else Color.LightGray
      )
    }
  }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun BrowserViewContentMain(
  viewModel: BrowserViewModel, browserMainView: BrowserMainView, pagerStateSearch: PagerState
) {
  Box(modifier = Modifier.fillMaxSize()) {
    BrowserMainView(viewModel)
  }
  if (!browserMainView.show.value) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.White.copy(pagerStateSearch.currentPageOffset))
    )
  }
}

@Composable
private fun BrowserViewContentWeb(viewModel: BrowserViewModel, browserWebView: BrowserWebView) {
  BackHandler {
    if (browserWebView.navigator.canGoBack) {
      viewModel.handleIntent(BrowserIntent.WebViewGoBack)
    }
  }
  val localFocusManager = LocalFocusManager.current
  key(browserWebView.webViewId) {
    Box(modifier = Modifier.fillMaxSize()) {
      WebView(
        state = browserWebView.state,
        navigator = browserWebView.navigator,
        factory = {
          browserWebView.webView.parent?.let { (it as ViewGroup).removeAllViews() }
          browserWebView.webView.also {
            it.setOnScrollChangeListener { view, scrollX, scrollY, oldScrollX, oldScrollY ->
              if (scrollY == 0 || oldScrollY == 0) return@setOnScrollChangeListener
              localFocusManager.clearFocus() // TODO 清除焦点
              if (oldScrollY < scrollY - 5 && browserWebView.showBottomBar.value) {
                browserWebView.showBottomBar.value = false // TODO 上滑，需要隐藏底部栏
              } else if (oldScrollY > scrollY + 5 && !browserWebView.showBottomBar.value) {
                browserWebView.showBottomBar.value = true // TODO 下滑，需要显示底部栏
              }
            }
          }
        }
      )
    }
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
private fun SearchBox(
  baseView: BrowserBaseView, showCamera: Boolean = false, search: (String) -> Unit
) {
  Box(
    modifier = Modifier
      .padding(horizontal = dimenSearchHorizontalAlign, vertical = dimenSearchVerticalAlign)
      .fillMaxWidth()
      .shadow(
        elevation = dimenShadowElevation,
        shape = RoundedCornerShape(dimenSearchRoundedCornerShape)
      )
      .height(dimenSearchHeight)
      .clip(RoundedCornerShape(dimenSearchRoundedCornerShape))
      .background(Color.White)
  ) {
    val inputText = when (baseView) {
      is BrowserWebView -> {
        ShowLinearProgressIndicator(baseView)
        mutableStateOf(baseView.state.lastLoadedUrl ?: "")
      }
      else -> mutableStateOf("")
    }
    SearchTextField(inputText, showCamera, baseView.focus, search)
    // SearchText(inputText, showCamera, baseView.focus, focusRequester)
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
private fun SearchTextField(
  inputText: MutableState<String>,
  showCamera: Boolean = false,
  focus: MutableState<Boolean>,
  search: (String) -> Unit
) {
  val focusManager = LocalFocusManager.current
  val currentText = remember { mutableStateOf(if (focus.value) inputText.value else "") }

  BasicTextField(
    value = currentText.value,
    onValueChange = { currentText.value = it },
    readOnly = false,
    enabled = true,
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = dimenSearchVerticalAlign)
      .onFocusChanged {
        focus.value = it.isFocused
        if (!it.isFocused) {
          currentText.value = ""
        } else {
          currentText.value = parseInputText(inputText.value, host = false) ?: inputText.value
        }
      },
    singleLine = true,
    textStyle = TextStyle.Default.copy(
      color = MaterialTheme.colors.onPrimary, fontSize = dimenTextFieldFontSize
    ),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = {
      if (currentText.value.isEmpty()) return@KeyboardActions
      if (currentText.value != inputText.value) {
        val requestUrl = Uri.parse(currentText.value)?.let { uri ->
          if ((uri.scheme == "http" || uri.scheme == "https") && uri.host?.isNotEmpty() == true) {
            currentText.value
          } else null
        } ?: "https://cn.bing.com/search?q=${currentText.value}"
        search(requestUrl)
      }
      focusManager.clearFocus() // 取消聚焦，就会间接的隐藏键盘
    })
  ) { innerTextField ->
    Box {
      Surface(modifier = Modifier.align(Alignment.Center)) {
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          AnimatedVisibility(visible = !focus.value) {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = null,
              tint = MaterialTheme.colors.onSecondary
            )
          }
          Box(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = dimenSearchHorizontalAlign)
          ) {
            if ((focus.value && currentText.value.isEmpty()) ||
              (!focus.value && inputText.value.isEmpty())
            ) {
              Text(
                text = stringResource(id = R.string.browser_search_hint),
                color = MaterialTheme.colors.onSecondary,
                modifier = Modifier.align(Alignment.CenterStart),
                fontSize = dimenTextFieldFontSize
              )
            } else if (!focus.value) {
              parseInputText(inputText.value)?.let { text ->
                Text(
                  text = text,
                  color = MaterialTheme.colors.onSecondary,
                  modifier = Modifier.align(Alignment.Center),
                  fontSize = dimenTextFieldFontSize
                )
              }
            }
            innerTextField()
          }

          if (currentText.value.isNotEmpty()) {
            Icon(imageVector = Icons.Outlined.Close,
              contentDescription = null,
              tint = MaterialTheme.colors.onSecondary,
              modifier = Modifier.clickable { currentText.value = "" })
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

/**
 * 根据内容来判断
 */
private fun parseInputText(text: String, host: Boolean = true): String? {
  val uri = Uri.parse(text)
  return if (uri.host == "cn.bing.com" && uri.path == "/search"
    && uri.getQueryParameter("q") != null
  ) {
    uri.getQueryParameter("q")
  } else {
    if (host) uri.host else text
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