package org.dweb_browser.browser.web.ui.browser.search

import android.annotation.SuppressLint
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import org.dweb_browser.browser.R
import org.dweb_browser.browser.web.ui.browser.dimenSearchHeight
import org.dweb_browser.browser.web.ui.browser.dimenTextFieldFontSize
import org.dweb_browser.browser.web.ui.browser.model.DefaultSearchWebEngine
import org.dweb_browser.browser.web.ui.browser.model.WebEngine
import org.dweb_browser.browser.web.ui.browser.model.findWebEngine
import org.dweb_browser.browser.web.ui.browser.model.parseInputText
import org.dweb_browser.browser.web.ui.browser.model.toRequestUrl
import org.dweb_browser.helper.compose.clickableWithNoEffect

/**
 * 组件： 搜索组件
 */
@Composable
internal fun SearchView(
  text: String,
  homePreview: (@Composable (onMove: (Boolean) -> Unit) -> Unit)? = null,
  searchPreview: (@Composable () -> Unit)? = null,
  onClose: () -> Unit,
  onSearch: (String) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  val inputText = remember { mutableStateOf(parseInputText(text, false)) }
  val searchPreviewState = remember { MutableTransitionState(text.isNotEmpty()) }
  val webEngine = findWebEngine(text)

  BackHandler {
    focusManager.clearFocus()
    onClose()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .clickableWithNoEffect { focusManager.clearFocus(); onClose() }
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.background)
    ) {
      homePreview?.let { it { moved -> focusManager.clearFocus(); if (!moved) onClose() } }

      Text(
        text = stringResource(id = R.string.browser_search_cancel),
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(20.dp)
          .clickable { onClose() },
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary
      )

      searchPreview?.let { it() } ?: SearchPreview(
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

    key(inputText) {
      BrowserTextField(
        text = inputText,
        webEngine = webEngine,
        onSearch = { onSearch(it) },
        onValueChanged = { inputText.value = it; searchPreviewState.targetState = it.isNotEmpty() }
      )
    }
  }
}

@Composable
internal fun BoxScope.BrowserTextField(
  text: MutableState<String>,
  webEngine: WebEngine?,
  onSearch: (String) -> Unit,
  onValueChanged: (String) -> Unit
) {
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  var inputText by remember { mutableStateOf(text.value) }
  //val localShowIme = LocalShowIme.current

  LaunchedEffect(focusRequester) {
    delay(100)
    focusRequester.requestFocus()
  }

  CustomTextField(
    value = inputText,
    onValueChange = { inputText = it.trim(); onValueChanged(inputText) },
    modifier = Modifier
      .fillMaxWidth()
      //.navigationBarsPadding()
      //.imePadding()
      .background(MaterialTheme.colorScheme.background)
      .align(Alignment.BottomCenter)
      .padding(
        start = 25.dp,
        end = 25.dp,
        top = 10.dp,
        bottom = 10.dp, // if (localShowIme.value) 0.dp else 50.dp // 为了贴合当前的界面底部工具栏
      )
      .height(dimenSearchHeight)
      .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.background)
      .focusRequester(focusRequester)
      .onKeyEvent {
        if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
          inputText.toRequestUrl()?.let { url ->
            onSearch(url)
          } ?: webEngine?.let { webEngine ->
            onSearch(String.format(webEngine.format, inputText))
          } ?: focusManager.clearFocus(); keyboardController?.hide()
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
      Image(
        imageVector = ImageVector.vectorResource(R.drawable.ic_circle_close),
        contentDescription = "Close",
        modifier = Modifier
          .clickable { inputText = ""; onValueChanged(inputText) }
          .size(28.dp)
          .padding(4.dp)
      )
    },
    keyboardOptions = KeyboardOptions(
      // 旧版本判断，如果搜索过一次，那么就直接按照之前搜索的来搜索，不进行输入内容是否域名的判断
      // imeAction = if (webEngine != null || inputText.isUrlOrHost()) ImeAction.Search else ImeAction.Done
      imeAction = ImeAction.Search // 增加上面的切换功能，会引起荣耀手机输入法异常多输出一个空格。
    ),
    keyboardActions = KeyboardActions(
      // onDone = { focusManager.clearFocus(); keyboardController?.hide() },
      onSearch = {
        // 如果内容符合地址，直接进行搜索，其他情况就按照如果有搜索引擎就按照搜索引擎来，没有的就隐藏键盘
        inputText.toRequestUrl()?.let { url ->
          onSearch(url)
        } ?: webEngine?.let { webEngine ->
          onSearch(String.format(webEngine.format, inputText))
        } ?: focusManager.clearFocus(); keyboardController?.hide()
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
  spacerWidth: Dp = 10.dp,
) {
  BasicTextField(
    value = value,
    onValueChange = { onValueChange(it) },
    modifier = modifier,
    maxLines = 1,
    singleLine = true,
    textStyle = TextStyle.Default.copy(
      fontSize = dimenTextFieldFontSize,
      color = MaterialTheme.colorScheme.onSecondaryContainer
    ),
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
  ) { innerTextField ->
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(spacerWidth))
        if (leadingIcon != null) {
          leadingIcon()
          Spacer(modifier = Modifier.width(spacerWidth))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
          innerTextField()
          if (label != null && value.isEmpty()) label() // 如果内容是空的才显示
        }
        if (trailingIcon != null) {
          Spacer(modifier = Modifier.width(spacerWidth))
          trailingIcon()
        }
        Spacer(modifier = Modifier.width(spacerWidth))
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
  if (show.targetState) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 20.dp)
        .clickableWithNoEffect { }
    ) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
        ) {
          Text(
            text = stringResource(id = R.string.browser_search_title),
            modifier = Modifier.align(Alignment.Center),
            fontSize = 20.sp
          )

          Text(
            text = stringResource(id = R.string.browser_search_cancel),
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
      text = stringResource(id = R.string.browser_search_engine),
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
        if (index > 0) VerticalDivider()
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