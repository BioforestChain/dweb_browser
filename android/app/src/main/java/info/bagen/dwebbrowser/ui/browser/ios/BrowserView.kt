package info.bagen.dwebbrowser.ui.browser.ios

import android.annotation.SuppressLint
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.web.LoadingState
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.ui.entity.BrowserBaseView
import info.bagen.dwebbrowser.ui.entity.BrowserMainView
import info.bagen.dwebbrowser.ui.entity.BrowserWebView
import info.bagen.dwebbrowser.ui.theme.Blue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val dimenTextFieldFontSize = 16.sp
internal val dimenSearchHorizontalAlign = 5.dp
internal val dimenSearchVerticalAlign = 10.dp
internal val dimenSearchRoundedCornerShape = 8.dp
internal val dimenShadowElevation = 4.dp
internal val dimenHorizontalPagerHorizontal = 20.dp
internal val dimenBottomHeight = 100.dp
internal val dimenSearchHeight = 40.dp
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
fun BrowserView(viewModel: BrowserViewModel) {
  Box(modifier = Modifier.fillMaxSize()) {
    BrowserViewContent(viewModel)   // 中间主体部分
    BrowserSearchPreview(viewModel) // 地址栏输入内容后，上面显示的书签、历史和相应搜索引擎
    BrowserViewBottomBar(viewModel) // 工具栏，包括搜索框和导航栏
    BrowserPopView(viewModel)       // 用于处理弹出框
    BrowserMultiPopupView(viewModel)// 用于显示多界面
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
        is BrowserMainView -> BrowserViewContentMain(viewModel, item)
        is BrowserWebView -> BrowserViewContentWeb(viewModel, item)
      }
    }
  }
}

@Composable
fun ColumnScope.MiniTitle(viewModel: BrowserViewModel) {
  val inputText = when (val browserBaseView = viewModel.uiState.currentBrowserBaseView.value) {
    is BrowserWebView -> {
      parseInputText(browserBaseView.state.lastLoadedUrl ?: "")
        ?: stringResource(id = R.string.browser_search_hint)
    }
    else -> stringResource(id = R.string.browser_search_hint)
  }

  Text(
    text = inputText,
    fontSize = 12.sp,
    modifier = Modifier.align(Alignment.CenterHorizontally)
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
      modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
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
  LaunchedEffect(PagerState) { // 为了修复隐藏搜索框后，重新加载时重新显示的问题，会显示第一页
    delay(100)
    viewModel.uiState.pagerStateNavigator.scrollToPage(viewModel.uiState.pagerStateNavigator.settledPage)
  }
  HorizontalPager(
    state = viewModel.uiState.pagerStateNavigator,
    pageCount = viewModel.uiState.browserViewList.size,
    contentPadding = PaddingValues(horizontal = dimenHorizontalPagerHorizontal),
  ) { currentPage ->
    when (val item = viewModel.uiState.browserViewList[currentPage]) {
      is BrowserWebView -> BrowserViewSearchWeb(viewModel, item)
      is BrowserMainView -> BrowserViewSearchMain(
        viewModel, item, viewModel.uiState.pagerStateNavigator
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserViewNavigatorBar(viewModel: BrowserViewModel) {
  val scope = rememberCoroutineScope()
  Row(modifier = Modifier.fillMaxWidth().height(dimenSearchHeight)) {
    val navigator = when(val item = viewModel.uiState.currentBrowserBaseView.value) {
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
    NavigatorButton(
      resId = navigator?.let { R.drawable.ic_main_add } ?: R.drawable.ic_main_qrcode_scan,
      resName = navigator?.let { R.string.browser_nav_add } ?: R.string.browser_nav_scan,
      show = true
    ) {  }
    NavigatorButton(
      resId = R.drawable.ic_main_multi, resName = R.string.browser_nav_multi, show = true
    ) {
      viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(true))
    }
    NavigatorButton(
      resId = R.drawable.ic_main_option, resName = R.string.browser_nav_option, show = true
    ) {
      scope.launch {
        viewModel.uiState.openBottomSheet.value = true
        delay(8)
        viewModel.uiState.modalBottomSheetState.partialExpand()
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
    .padding(horizontal = 2.dp)
    .clickable(enabled = show) { onClick() }) {
    Column(modifier = Modifier.align(Alignment.Center)) {
      Icon(
        modifier = Modifier.size(28.dp),
        imageVector = ImageVector.vectorResource(id = resId),//ImageBitmap.imageResource(id = resId),
        contentDescription = stringResource(id = resName),
        tint = if (show) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
      )
    }
  }
}

@Composable
private fun BrowserViewContentMain(viewModel: BrowserViewModel, browserMainView: BrowserMainView) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(bottom = dimenBottomHeight)
  ) {
    BrowserMainView(viewModel, browserMainView)
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
private fun SearchBox(
  viewModel: BrowserViewModel,
  baseView: BrowserBaseView,
  showCamera: Boolean = false,
  search: (String) -> Unit
) {
  Box(
    modifier = Modifier
      .padding(
        horizontal = dimenSearchHorizontalAlign, vertical = dimenSearchVerticalAlign
      )
      .fillMaxWidth()
      .shadow(
        elevation = dimenShadowElevation,
        shape = RoundedCornerShape(dimenSearchRoundedCornerShape)
      )
      .height(dimenSearchHeight)
      .clip(RoundedCornerShape(dimenSearchRoundedCornerShape))
      .background(MaterialTheme.colorScheme.background)
  ) {
    val inputText = when (baseView) {
      is BrowserWebView -> {
        ShowLinearProgressIndicator(baseView)
        mutableStateOf(baseView.state.lastLoadedUrl ?: "")
      }
      else -> mutableStateOf("")
    }
    SearchTextField(viewModel, inputText, showCamera, baseView.focus, search)
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
          color = MaterialTheme.colorScheme.primary
        )
      }
      else -> {}
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchTextField(
  viewModel: BrowserViewModel,
  inputText: MutableState<String>,
  showCamera: Boolean = false,
  focus: MutableState<Boolean>,
  search: (String) -> Unit
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val currentText = remember { mutableStateOf(if (focus.value) inputText.value else "") }

  BasicTextField(
    value = currentText.value,
    onValueChange = {
      currentText.value = it
      viewModel.handleIntent(BrowserIntent.UpdateSearchEngineState(it.isNotEmpty()))
      viewModel.handleIntent(BrowserIntent.UpdateInputText(it))
    },
    readOnly = false,
    enabled = true,
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = dimenSearchVerticalAlign)
      .onFocusChanged {
        focus.value = it.isFocused
        val text = if (!it.isFocused) {
          ""
        } else {
          parseInputText(inputText.value, host = false) ?: inputText.value
        }
        currentText.value = text
        viewModel.handleIntent(BrowserIntent.UpdateInputText(text))
      },
    singleLine = true,
    textStyle = TextStyle.Default.copy(
      /*color = MaterialTheme.colorScheme.onPrimary, */fontSize = dimenTextFieldFontSize
    ),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done/*ImeAction.Search*/),
    keyboardActions = KeyboardActions(
      onDone = {
        keyboardController?.hide()
      },
      onSearch = {
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
      }
    )
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
              tint = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.align(Alignment.CenterStart),
                fontSize = dimenTextFieldFontSize
              )
            } else if (!focus.value) {
              parseInputText(inputText.value)?.let { text ->
                Text(
                  text = text,
                  color = MaterialTheme.colorScheme.surfaceVariant,
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
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(25.dp).clickable {
                currentText.value = ""
                viewModel.handleIntent(BrowserIntent.UpdateInputText(""))
                viewModel.handleIntent(BrowserIntent.UpdateSearchEngineState(false))
              })
          }/* else if (showCamera) {
            Icon(
              imageVector = ImageVector.vectorResource(id = R.drawable.ic_photo_camera_24),
              contentDescription = null,
              tint = MaterialTheme.colors.onSecondary,
            )
          }*/
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
  SearchBox(viewModel, browserMainView, showCamera = true) { url ->
    viewModel.handleIntent(BrowserIntent.UpdateSearchEngineState(false)) // 隐藏搜索内容
    viewModel.handleIntent(BrowserIntent.AddNewWebView(url))
  }
}

@Composable
private fun BrowserViewSearchWeb(viewModel: BrowserViewModel, browserWebView: BrowserWebView) {
  SearchBox(viewModel, browserWebView, showCamera = false) { url ->
    viewModel.handleIntent(BrowserIntent.UpdateSearchEngineState(false)) // 隐藏搜索内容
    viewModel.handleIntent(BrowserIntent.SearchWebView(url))
  }
}