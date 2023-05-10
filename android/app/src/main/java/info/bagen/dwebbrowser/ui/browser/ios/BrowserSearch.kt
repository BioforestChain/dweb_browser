package info.bagen.dwebbrowser.ui.browser.ios

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.datastore.DefaultSearchWebEngine
import info.bagen.dwebbrowser.ui.entity.BrowserWebView
import info.bagen.dwebbrowser.ui.entity.WebSiteInfo
import kotlinx.coroutines.delay

/**
 * 提供给外部调用的  搜索界面，可以含有BrowserViewModel
 */
@Composable
fun BrowserSearchView(viewModel: BrowserViewModel) {
  if (viewModel.uiState.showSearchView.value) {
    val inputText = viewModel.uiState.inputText.value
    val text = if (inputText.startsWith("file:///android_asset") ||
      inputText == stringResource(id = R.string.browser_search_hint)
    ) {
      ""
    } else {
      inputText
    }
    val imeShowed = remember { mutableStateOf(false) }

    LaunchedEffect(imeShowed) {
      snapshotFlow { viewModel.uiState.currentInsets.value }.collect {
        imeShowed.value = it.getInsets(WindowInsetsCompat.Type.ime()).bottom > 0
      }
    }

    SearchView(text = text, imeShowed = imeShowed, onClose = {
      viewModel.uiState.showSearchView.value = false
    }, onSearch = { url -> // 第一个是搜索关键字，第二个是搜索地址
      viewModel.uiState.showSearchView.value = false
      viewModel.saveLastKeyword(url)
      viewModel.handleIntent(BrowserIntent.SearchWebView(url))
    })
  }
}

/**
 * 组件： 搜索组件
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchView(
  text: String,
  imeShowed: MutableState<Boolean> = mutableStateOf(false),
  onClose: () -> Unit,
  onSearch: (String) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  var inputText by remember { mutableStateOf(parseInputText(text, false)) }
  val focusRequester = remember { FocusRequester() }
  val searchPreviewState = remember { MutableTransitionState(false) }
  val webEngine = findWebEngine(text)

  LaunchedEffect(focusRequester) {
    delay(100)
    focusRequester.requestFocus()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background.copy(0.5f))
      .clickable(indication = null, onClick = {
        focusManager.clearFocus()
        onClose()
      }, interactionSource = remember { MutableInteractionSource() })
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(bottom = dimenBottomHeight)
    ) {
      //HomePage()
      SearchPreview(show = searchPreviewState,
        text = inputText,
        onClose = {
          focusManager.clearFocus()
          onClose()
        },
        onSearch = {
          focusManager.clearFocus()
          onSearch(it)
        })
    }

    CustomTextField(
      value = inputText,
      onValueChange = {
        inputText = it
        searchPreviewState.targetState = it.isNotEmpty()
      },
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .navigationBarsPadding()
        .imePadding()
        .align(Alignment.BottomCenter)
        .padding(
          start = 25.dp,
          end = 25.dp,
          top = 10.dp,
          bottom = if (imeShowed.value) 0.dp else 50.dp // 为了贴合当前的界面底部工具栏
        )
        .height(dimenSearchHeight)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.background)
        .focusRequester(focusRequester),
      label = {
        Text(
          text = stringResource(id = R.string.browser_search_hint),
          fontSize = dimenTextFieldFontSize,
          textAlign = TextAlign.Start,
          maxLines = 1
        )
      },
      trailingIcon = {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Close",
          modifier = Modifier.clickable { inputText = ""; searchPreviewState.targetState = false }
        )
      },
      keyboardOptions = KeyboardOptions(
        imeAction = webEngine?.let { ImeAction.Search } ?: ImeAction.Done
      ),
      keyboardActions = KeyboardActions(
        onDone = { keyboardController?.hide() },
        onSearch = {
          webEngine?.let { onSearch(String.format(it.format, inputText)) }
        }
      )
    )
  }
}

@Composable
private fun CustomTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
  BasicTextField(
    value = value,
    onValueChange = { onValueChange(it) },
    modifier = modifier,
    maxLines = 1,
    singleLine = true,
    textStyle = TextStyle.Default.copy(fontSize = dimenTextFieldFontSize),
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
  ) { innerTextField ->
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
      Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(10.dp))
        if (leadingIcon != null) {
          leadingIcon()
          Spacer(modifier = Modifier.width(10.dp))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
          innerTextField()
          if (label != null && value.isEmpty()) label() // 如果内容是空的才显示
        }
        if (trailingIcon != null) {
          Spacer(modifier = Modifier.width(10.dp))
          trailingIcon()
        }
        Spacer(modifier = Modifier.width(10.dp))
      }

    }
  }
}

@SuppressLint("NewApi")
@Composable
private fun SearchPreview( // 输入搜索内容后，显示的搜索信息
  show: MutableTransitionState<Boolean>,
  text: String,
  onClose: () -> Unit,
  onSearch: (String) -> Unit
) {
  if (show.targetState) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.outlineVariant)
        .padding(horizontal = 20.dp)
    ) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        ) {
          Text(text = "搜索", modifier = Modifier.align(Alignment.Center), fontSize = 16.sp)
          Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_close),
            contentDescription = "Close",
            modifier = Modifier
              .padding(end = 16.dp)
              .size(24.dp)
              .align(Alignment.CenterEnd)
              .clickable { onClose() })
        }
      }
      // 1. 标签页中查找关键字， 2. 搜索引擎， 3. 历史记录， 4.页内查找
      item { // 标签页中查找
        //SearchItemForTab(text)
      }
      item { // 搜索引擎
        SearchItemEngines(text) { onSearch(it) }
      }
      item { // 历史记录
        //SearchItemHistory(text)
      }
    }
  }
}

@Composable
private fun SearchItemForTab(viewModel: BrowserViewModel, text: String) {
  var firstIndex: Int? = null
  viewModel.uiState.browserViewList.filterIndexed { index, browserBaseView ->
    if (browserBaseView is BrowserWebView && browserBaseView.state.pageTitle?.contains(text) == true) {
      if (firstIndex == null) firstIndex = index
      true
    } else {
      false
    }
  }.firstOrNull()?.also { browserBaseView ->
    if (browserBaseView === viewModel.uiState.currentBrowserBaseView.value) return@also // TODO 如果搜索到的界面就是我当前显示的界面，就不显示该项
    val website = (browserBaseView as BrowserWebView).state.let {
      WebSiteInfo(it.pageTitle ?: "无标题", it.lastLoadedUrl ?: "localhost")
    }
    SearchWebsiteCardView(webSiteInfo = website, drawableId = R.drawable.ic_main_multi) {
      // TODO 调转到指定的标签页
      viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(false, firstIndex))
    }
  }
}

@Composable
private fun SearchItemEngines(text: String, onSearch: (String) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "搜索引擎",
      color = MaterialTheme.colorScheme.outline,
      modifier = Modifier.padding(vertical = 10.dp)
    )
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.background)
    ) {
      DefaultSearchWebEngine.forEachIndexed { index, webEngine ->
        if (index > 0) {
          Divider()
        }
        Card(modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .clickable { onSearch(String.format(webEngine.format, text)) }) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(
              model = R.drawable.ic_web,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = webEngine.name,
                //color = MaterialTheme.colorScheme.surfaceTint,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
              )
              Text(
                text = text,
                color = MaterialTheme.colorScheme.outlineVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }

  }
}

@Composable
private fun SearchItemHistory(viewModel: BrowserViewModel, text: String) {
  viewModel.uiState.historyWebsiteMap.firstNotNullOfOrNull {
    it.value.find { websiteInfo -> websiteInfo.title.contains(text) }
  }?.also { websiteInfo ->
    Spacer(modifier = Modifier.height(10.dp))
    SearchWebsiteCardView(websiteInfo, drawableId = R.drawable.ic_main_history) {
      // TODO 调转到指定的标签页
      viewModel.handleIntent(BrowserIntent.SearchWebView(websiteInfo.url))
    }
  }
}

@Composable
fun SearchWebsiteCardView(
  webSiteInfo: WebSiteInfo,
  @DrawableRes drawableId: Int = R.drawable.ic_web,
  onClick: () -> Unit
) {
  Card(modifier = Modifier
    .fillMaxWidth()
    .height(50.dp)
    .clickable { onClick() }) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = webSiteInfo.icon ?: drawableId,
        contentDescription = null,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = webSiteInfo.title,
          //color = MaterialTheme.colorScheme.surfaceTint,
          fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
          text = webSiteInfo.url,
          color = MaterialTheme.colorScheme.outlineVariant,
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

