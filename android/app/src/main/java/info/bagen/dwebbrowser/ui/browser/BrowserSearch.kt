package info.bagen.dwebbrowser.ui.browser

import android.annotation.SuppressLint
import android.util.Log
import android.view.KeyEvent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
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
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.database.WebSiteDatabase
import info.bagen.dwebbrowser.database.WebSiteInfo
import info.bagen.dwebbrowser.database.WebSiteType
import info.bagen.dwebbrowser.datastore.DefaultSearchWebEngine
import info.bagen.dwebbrowser.datastore.WebEngine
import info.bagen.dwebbrowser.ui.entity.BrowserWebView
import io.ktor.util.reflect.*
import kotlinx.coroutines.delay

/**
 * 组件： 搜索组件
 */
@Composable
internal fun SearchView(
  text: String,
  imeShowed: MutableState<Boolean> = mutableStateOf(false),
  homePreview: (@Composable () -> Unit)? = null,
  searchPreview: (@Composable () -> Unit)? = null,
  onClose: () -> Unit,
  onSearch: (String) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  val inputText = remember { mutableStateOf(parseInputText(text, false)) }
  val searchPreviewState = remember { MutableTransitionState(false) }
  val webEngine = findWebEngine(text)
  Log.e("lin.huang", "SearchView enter")

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background.copy(0.5f))
      .clickable(
        indication = null,
        onClick = { /*focusManager.clearFocus(); onClose()*/ },
        interactionSource = remember { MutableInteractionSource() }
      )
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(bottom = dimenBottomHeight)
    ) {
      homePreview?.let { it() }

      Text(
        text = "取消",
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(20.dp)
          .clickable { onClose() },
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary
      )

      searchPreview?.let { it() }
        ?: SearchPreview(
          show = searchPreviewState,
          text = inputText,
          onClose = {
            focusManager.clearFocus()
            onClose()
          },
          onSearch = {
            focusManager.clearFocus()
            onSearch(it)
          }
        )
    }

    BrowserTextField(
      text = inputText,
      webEngine = webEngine,
      imeShowed = imeShowed,
      onSearch = { onSearch(it) },
      onValueChanged = { inputText.value = it; searchPreviewState.targetState = it.isNotEmpty() }
    )
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun BoxScope.BrowserTextField(
  text: MutableState<String>,
  webEngine: WebEngine?,
  imeShowed: MutableState<Boolean>,
  onSearch: (String) -> Unit,
  onValueChanged: (String) -> Unit
) {
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  var inputText by remember { mutableStateOf(text.value) }

  LaunchedEffect(focusRequester) {
    delay(100)
    focusRequester.requestFocus()
  }

  CustomTextField(
    value = inputText,
    onValueChange = { inputText = it.trim(); onValueChanged(inputText) },
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
      .focusRequester(focusRequester)
      .onKeyEvent {
        if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
          webEngine?.let { webEngine ->
            onSearch(String.format(webEngine.format, inputText))
          } ?: if (inputText.isUrlOrHost()) {
            onSearch(inputText.toRequestUrl())
          } else {
            focusManager.clearFocus(); keyboardController?.hide()
          }
          true
        } else {
          false
        }
      },
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
        imageVector = ImageVector.vectorResource(R.drawable.ic_circle_close),
        contentDescription = "Close",
        modifier = Modifier
          .clickable { inputText = ""; onValueChanged(inputText) }
          .size(28.dp)
          .padding(4.dp)
      )
    },
    keyboardOptions = KeyboardOptions(
      imeAction = if (webEngine != null || inputText.isUrlOrHost()) ImeAction.Search else ImeAction.Done
    ),
    keyboardActions = KeyboardActions(
      onDone = { focusManager.clearFocus(); keyboardController?.hide() },
      onSearch = {
        webEngine?.let { onSearch(String.format(it.format, inputText)) }
          ?: onSearch(inputText.toRequestUrl())
      }
    )
  )
}

@Composable
fun CustomTextField(
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
internal fun SearchPreview( // 输入搜索内容后，显示的搜索信息
  show: MutableTransitionState<Boolean>,
  text: MutableState<String>,
  onClose: () -> Unit,
  onSearch: (String) -> Unit
) {
  Log.e("lin.huang", "SearchPreview enter")
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
            .padding(vertical = 20.dp)
        ) {
          Text(text = "搜索", modifier = Modifier.align(Alignment.Center), fontSize = 20.sp)

          Text(
            text = "取消",
            modifier = Modifier
              .align(Alignment.TopEnd)
              .clickable { onClose() },
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
      item { // 搜索引擎
        SearchItemEngines(text.value) { onSearch(it) }
      }
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
        if (index > 0) Divider()
        androidx.compose.material3.ListItem(
          headlineContent = {
            Text(text = webEngine.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
          },
          modifier = Modifier.clickable { onSearch(String.format(webEngine.format, text)) },
          supportingContent = {
            Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
          },
          leadingContent = {
            AsyncImage(
              model = webEngine.iconRes,
              contentDescription = null,
              modifier = Modifier.size(40.dp)
            )
          }
        )
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
      WebSiteInfo(
        title = it.pageTitle ?: "无标题",
        url = it.lastLoadedUrl ?: "localhost",
        type = WebSiteType.Multi
      )
    }
    SearchWebsiteCardView(webSiteInfo = website, drawableId = R.drawable.ic_main_multi) {
      // TODO 调转到指定的标签页
      viewModel.handleIntent(BrowserIntent.UpdateMultiViewState(false, firstIndex))
    }
  }
}


@Composable
private fun SearchItemHistory(text: String, onSearch: (String) -> Unit) {
  val list = remember { mutableListOf<WebSiteInfo>() }
  LaunchedEffect(list) {
    WebSiteDatabase.INSTANCE.websiteDao().findByNameTop10(text).observeForever {
      list.clear()
      it.forEach { webSiteInfo ->
        list.add(webSiteInfo)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.background)
  ) {
    list.forEachIndexed { index, webSiteInfo ->
      if (index > 0) Divider()
      SearchWebsiteCardView(webSiteInfo, drawableId = R.drawable.ic_main_history) {
        // TODO 调转到指定的标签页
        onSearch(webSiteInfo.url)
      }
    }
  }
}

@Composable
private fun SearchWebsiteCardView(
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