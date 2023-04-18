package info.bagen.dwebbrowser.ui.browser

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.web.LoadingState
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.ui.theme.Blue
import kotlinx.coroutines.delay

private val dimenTextFieldFontSize = 16.sp
private val dimenSearchHorizontalAlign = 5.dp
private val dimenSearchVerticalAlign = 10.dp
private val dimenSearchRoundedCornerShape = 8.dp
private val dimenShadowElevation = 4.dp
private val dimenHorizontalPagerHorizontal = 20.dp
private val dimenBottomHeight = 100.dp
private val dimenSearchHeight = 40.dp
private val dimenMinBottomHeight = 20.dp

private val bottomEnterAnimator = slideInVertically(animationSpec = tween(300),//动画时长1s
  initialOffsetY = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  })
private val bottomExitAnimator = slideOutVertically(animationSpec = tween(300),//动画时长1s
  targetOffsetY = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  })

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserView(viewModel: BrowserViewModel) {
  LaunchedEffect(viewModel.uiState) { // 用于截图，滑动界面后，截图操作
    snapshotFlow { viewModel.uiState.pagerStateNavigator.settledPage }.collect {
      viewModel.handleIntent(BrowserIntent.PictureCapture(it))
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    BrowserViewContent(viewModel)
    BrowserViewBottomBar(viewModel)
    BrowserPopView(viewModel)
    BrowserMultiPopupView(viewModel)
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserViewContent(viewModel: BrowserViewModel) {
  val localFocusManager = LocalFocusManager.current
  LaunchedEffect(viewModel.uiState.pagerStateNavigator) {
    snapshotFlow { viewModel.uiState.pagerStateNavigator.currentPageOffsetFraction }.collect { currentPageOffset ->
      viewModel.uiState.pagerStateContent.scrollToPage(
        viewModel.uiState.pagerStateNavigator.currentPage, currentPageOffset
      )
    }
  }
  LaunchedEffect(viewModel.uiState.pagerStateContent) {
    snapshotFlow { viewModel.uiState.pagerStateContent.currentPage }.collect { currentPage ->
      viewModel.handleIntent(BrowserIntent.UpdateCurrentBaseView(currentPage))
    }
  }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .clickable(indication = null,
        onClick = { localFocusManager.clearFocus() },
        interactionSource = remember { MutableInteractionSource() })
  ) {
    // 创建一个不可滑动的 HorizontalPager , 然后由底下的 Search 来控制滑动效果
    HorizontalPager(
      state = viewModel.uiState.pagerStateContent,
      pageCount = viewModel.uiState.browserViewList.size,
      beyondBoundsPageCount = 5,
      userScrollEnabled = false
    ) { currentPage ->
      when (val item = viewModel.uiState.browserViewList[currentPage]) {
        is BrowserMainView -> BrowserViewContentMain(
          viewModel, item, viewModel.uiState.pagerStateNavigator
        )
        is BrowserWebView -> BrowserViewContentWeb(viewModel, item)
      }
    }
  }
}

@Composable
private fun BoxScope.BrowserViewBottomBar(viewModel: BrowserViewModel) {
  val browserBaseView = viewModel.uiState.currentBrowserBaseView.value
  val inputText = when (browserBaseView) {
    is BrowserWebView -> {
      parseInputText(browserBaseView.state.lastLoadedUrl ?: "")
        ?: stringResource(id = R.string.browser_search_hint)
    }
    else -> stringResource(id = R.string.browser_search_hint)
  }

  Box(modifier = Modifier.align(Alignment.BottomCenter)) {
    Column(modifier = Modifier
      .fillMaxWidth()
      .height(dimenMinBottomHeight)
      .align(Alignment.BottomCenter)
      .clickable { viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(true)) }) {
      Text(
        text = inputText, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally)
      )
    }

    AnimatedVisibility(
      visibleState = browserBaseView.showBottomBar,
      enter = bottomEnterAnimator,
      exit = bottomExitAnimator
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        BrowserViewSearch(viewModel)
        BrowserViewNavigatorBar(viewModel) {
          // 下面是切换主页的
          /*coroutineScope.launch {
            pagerStateSearch.animateScrollToPage(viewModel.uiState.browserViewList.size)
            pagerStateWebView.animateScrollToPage(viewModel.uiState.browserViewList.size)
          }*/
          viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(true))
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserViewSearch(viewModel: BrowserViewModel) {
  LaunchedEffect(PagerState) { // 为了修复隐藏搜索框后，重新加载时重新显示的问题，会显示第一页
    delay(100)
    viewModel.uiState.pagerStateNavigator.scrollToPage(viewModel.uiState.pagerStateNavigator.settledPage)
  }
  HorizontalPager(
    state = viewModel.uiState.pagerStateNavigator,
    pageCount = viewModel.uiState.browserViewList.size,
    contentPadding = PaddingValues(horizontal = dimenHorizontalPagerHorizontal),
    modifier = Modifier.background(MaterialTheme.colors.primaryVariant)
  ) { currentPage ->
    when (val item = viewModel.uiState.browserViewList[currentPage]) {
      is BrowserWebView -> BrowserViewSearchWeb(viewModel, item)
      is BrowserMainView -> BrowserViewSearchMain(
        viewModel, item, viewModel.uiState.pagerStateNavigator
      )
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
      show = navigator?.let { true } ?: false) { /* TODO 将当前的地址添加到书签 */ }
    NavigatorButton(
      resId = R.drawable.ic_main_menu, resName = R.string.browser_nav_option, show = true
    ) {
      // TODO 打开弹窗，里面有历史浏览记录和书签列表
      viewModel.handleIntent(BrowserIntent.UpdatePopupViewState(PopupViewSate.Options))
    }
    NavigatorButton(
      resId = R.drawable.ic_main_multi,
      resName = R.string.browser_nav_home,
      show = true //navigator?.let { true } ?: false
    ) {
      onHome() // 打开主页
    }
  }
}

@Composable
private fun RowScope.NavigatorButton(
  @DrawableRes resId: Int, @StringRes resName: Int, show: Boolean, onClick: () -> Unit
) {
  Box(modifier = Modifier
    .weight(1f)
    .padding(horizontal = 2.dp)
    .clickable(enabled = show) { onClick() }) {
    Column(modifier = Modifier.align(Alignment.Center)) {
      Icon(
        modifier = Modifier.size(28.dp),
        imageVector = ImageVector.vectorResource(id = resId),//ImageBitmap.imageResource(id = resId),
        contentDescription = stringResource(id = resName),
        tint = if (show) Blue else Color.LightGray
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserViewContentMain(
  viewModel: BrowserViewModel, browserMainView: BrowserMainView, pagerStateSearch: PagerState
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(bottom = dimenBottomHeight)
  ) {
    BrowserMainView(viewModel, browserMainView)
  }
  /*if (!browserMainView.show.value) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.White.copy(pagerStateSearch.currentPageOffset))
    )
  }*/
}

@Composable
private fun BrowserViewContentWeb(viewModel: BrowserViewModel, browserWebView: BrowserWebView) {
  BackHandler {
    if (browserWebView.navigator.canGoBack) {
      viewModel.handleIntent(BrowserIntent.WebViewGoBack)
    }
  }

  key(browserWebView.webViewId) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = if (browserWebView.showBottomBar.currentState) dimenBottomHeight else dimenHorizontalPagerHorizontal)
    ) {
      BrowserWebView(viewModel = viewModel, browserWebView = browserWebView)
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
      .padding(
        horizontal = dimenSearchHorizontalAlign, vertical = dimenSearchVerticalAlign
      )
      .fillMaxWidth()
      .shadow(
        elevation = dimenShadowElevation, shape = RoundedCornerShape(dimenSearchRoundedCornerShape)
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
            if ((focus.value && currentText.value.isEmpty()) || (!focus.value && inputText.value.isEmpty())) {
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
  return if (uri.host == "cn.bing.com" && uri.path == "/search" && uri.getQueryParameter("q") != null) {
    uri.getQueryParameter("q")
  } else {
    if (host) uri.host else text
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserViewSearchMain(
  viewModel: BrowserViewModel, browserMainView: BrowserMainView, pagerState: PagerState
) {
  SearchBox(browserMainView, showCamera = true) { url ->
    viewModel.handleIntent(BrowserIntent.AddNewWebView(url))
  }
  // TODO 这边考虑加一层遮罩，颜色随着滑动而显示
  if (!browserMainView.show.value) {
//    Box(
//      modifier = Modifier
//        .fillMaxSize()
//        .background(Color.White.copy(pagerState.currentPageOffsetFraction))
//    )
  }
}

@Composable
private fun BrowserViewSearchWeb(viewModel: BrowserViewModel, browserWebView: BrowserWebView) {
  SearchBox(browserWebView, showCamera = false) { url ->
    viewModel.handleIntent(BrowserIntent.SearchWebView(url))
  }
}