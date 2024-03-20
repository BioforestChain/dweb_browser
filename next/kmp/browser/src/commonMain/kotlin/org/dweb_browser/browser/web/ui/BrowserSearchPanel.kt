package org.dweb_browser.browser.web.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.search.SearchInject
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.helper.format
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.LocalWindowsImeVisible
import org.dweb_browser.sys.window.render.imageFetchHook

/**
 * 搜索界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSearchPanel(modifier: Modifier = Modifier) {
  val viewModel = LocalBrowserViewModel.current
  val searchPage = viewModel.showSearch
  AnimatedVisibility(
    searchPage != null,
    enter = slideInVertically(enterAnimationSpec()) { it },
    exit = slideOutVertically(exitAnimationSpec()) { it },
  ) {
    if (searchPage == null) {
      return@AnimatedVisibility
    }
    val focusManager = LocalFocusManager.current
    val hide = {
      focusManager.clearFocus()
      viewModel.showSearch = null
    }
    /// 返回关闭搜索
    LocalWindowController.current.GoBackHandler {
      hide()
    }
    val uiScope = rememberCoroutineScope()
    var searchTextField by remember(searchPage) {
      mutableStateOf(
        TextFieldValue(
          searchPage.url, selection = TextRange(0, searchPage.url.length)
        )
      )
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
      modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      contentWindowInsets = WindowInsets(0),
      topBar = {
        CenterAlignedTopAppBar(
          windowInsets = WindowInsets(0, 0, 0, 0), // 顶部
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
          ),
          title = {
            Text(
              BrowserI18nResource.browser_search_title(), overflow = TextOverflow.Ellipsis
            )
          },
          /// 左上角导航按钮
          navigationIcon = {
            IconButton(onClick = hide) {
              Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close Search Panel"
              )
            }
          },
          /// 右上角功能按钮
          actions = {
            // TODO 提供搜索的设置
          },
          scrollBehavior = scrollBehavior,
        )
      },
    ) { innerPadding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding)
      ) {
        /// 面板内容
        Box(Modifier.fillMaxWidth().weight(1f)) {
          if (searchTextField.text.isNotEmpty()) {
            SearchSuggestion(searchText = searchTextField.text,
              modifier = Modifier.fillMaxWidth(),
              onClose = hide,
              // 模拟
              onOpenApp = {
                // TODO 暂未实现
              },
              // 输入框输入提交
              onSubmit = { url ->
                uiScope.launch {
                  viewModel.doSearchUI(url)
                  hide() // 该操作需要在上面执行完成后执行，否则会导致uiScope被销毁，而不执行doSearchUI
                }
              })
          }
        }

        /// 底部的输入框
        Box(
          Modifier.fillMaxWidth().padding(dimenPageHorizontal)
        ) {
          val focusRequester = remember { FocusRequester() }
          LaunchedEffect(focusRequester) {
            delay(100)
            focusRequester.requestFocus()
          }
          val showIme by LocalWindowsImeVisible.current
          /// 键盘隐藏后，需要清除焦点，避免再次点击不显示键盘的问题
          /// 键盘显示出来的时候，默认要进行全选操作
          LaunchedEffect(showIme) {
            if (!showIme) {
              focusManager.clearFocus()
            } else {
              if (searchTextField.selection.start != 0 || searchTextField.selection.end != searchTextField.text.length) {
                searchTextField =
                  searchTextField.copy(selection = TextRange(0, searchTextField.text.length))
              }
            }
          }
          BasicTextField(
            value = searchTextField,
            onValueChange = {
              searchTextField = it
            },
            modifier = Modifier.fillMaxWidth().searchBoxStyle(SearchBoxTheme.Border)
              .focusRequester(focusRequester),
            maxLines = 1,
            singleLine = true,
            textStyle = TextStyle.Default.copy(
              fontSize = dimenTextFieldFontSize,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            keyboardOptions = KeyboardOptions(
              // 旧版本判断，如果搜索过一次，那么就直接按照之前搜索的来搜索，不进行输入内容是否域名的判断
              // imeAction = if (webEngine != null || inputText.isUrlOrHost()) ImeAction.Search else ImeAction.Done
              imeAction = ImeAction.Search // 增加上面的切换功能，会引起荣耀手机输入法异常多输出一个空格。
            ),
            keyboardActions = KeyboardActions(
              // onDone = { focusManager.clearFocus(); keyboardController?.hide() },
              onSearch = {
                // 如果内容符合地址，直接进行搜索，其他情况就按照如果有搜索引擎就按照搜索引擎来，没有的就隐藏键盘
                uiScope.launch {
                  val searchText = searchTextField.text.trim()
                  viewModel.findSearchEngine(searchText)?.let { searchEngine ->
                    viewModel.tryOpenUrlUI(searchEngine.homeLink, searchPage)
                  } ?: viewModel.tryOpenUrlUI(searchText, searchPage)
                  hide()
                }
              }),
          ) { innerTextField ->
            Row(
              modifier = Modifier.fillMaxSize(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier.weight(1f).searchInnerStyle(end = false),
                contentAlignment = Alignment.CenterStart,
              ) {
                innerTextField()
                /// Placeholder
                if (searchTextField.text.isEmpty()) {
                  Text(
                    text = BrowserI18nResource.browser_search_hint(),
                    fontSize = dimenTextFieldFontSize,
                    maxLines = 1,
                    modifier = Modifier.fillMaxSize().alpha(0.5f)
                  )
                }
              }
              // 清除文本的按钮
              IconButton({
                searchTextField = TextFieldValue("")
              }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear Input Text")
              }
            }
          }
        }
      }
    }

  }
}


//@Composable
//internal fun BrowserTextField(
//  modifier: Modifier,
//  inputField: TextFieldValue,
//  searchEngine: SearchEngine?,
//  onBlur: () -> Unit,
//  onSubmitSearch: (String) -> Unit,
//  onValueChanged: (String) -> Unit
//) {
//  val focusManager = LocalFocusManager.current
//  val keyboardController = LocalSoftwareKeyboardController.current
//  val viewModel = LocalBrowserViewModel.current
//  val uiScope = rememberCoroutineScope()
//
//  CustomTextField(value = inputField, onValueChange = {
//    val trimmedText = it.text.trim()
//    inputField = if (trimmedText == it.text) it else it.copy(text = trimmedText);
//    onValueChanged(
//      inputField
//    )
//  }, modifier = modifier.background(MaterialTheme.colorScheme.background).padding(
//    start = 25.dp,
//    end = 25.dp,
//    top = 10.dp,
//    bottom = 10.dp, // if (localShowIme.value) 0.dp else 50.dp // 为了贴合当前的界面底部工具栏
//  ).height(dimenSearchHeight)
//    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
//    .clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.background).onKeyEvent {
//      if (it.key == Key.Enter) {
//        inputField.toRequestUrl()?.let { url ->
//          onSubmitSearch(url)
//        } ?: searchEngine?.let { searchEngine ->
//          onSubmitSearch("${searchEngine.searchLink}$inputField")
//        } ?: focusManager.clearFocus(); keyboardController?.hide()
//        true
//      } else {
//        false
//      }
//    }, label = {
//    Text(
//      text = BrowserI18nResource.browser_search_hint(),
//      fontSize = dimenTextFieldFontSize,
//      textAlign = TextAlign.Start,
//      maxLines = 1
//    )
//  }, trailingIcon = {
//    Image(
//      imageVector = Icons.Default.Close,
//      contentDescription = "Close",
//      modifier = Modifier.clickable { inputField = ""; onValueChanged(inputField) }.size(28.dp)
//        .padding(4.dp)
//    )
//  }, keyboardOptions = KeyboardOptions(
//    // 旧版本判断，如果搜索过一次，那么就直接按照之前搜索的来搜索，不进行输入内容是否域名的判断
//    // imeAction = if (webEngine != null || inputText.isUrlOrHost()) ImeAction.Search else ImeAction.Done
//    imeAction = ImeAction.Search // 增加上面的切换功能，会引起荣耀手机输入法异常多输出一个空格。
//  ), keyboardActions = KeyboardActions(
//    // onDone = { focusManager.clearFocus(); keyboardController?.hide() },
//    onSearch = {
//      // 如果内容符合地址，直接进行搜索，其他情况就按照如果有搜索引擎就按照搜索引擎来，没有的就隐藏键盘
//      uiScope.launch {
//
//        inputField.toRequestUrl()?.let { url ->
//          onSubmitSearch(url)
//        } ?: searchEngine?.let { searchEngine ->
//          onSubmitSearch("${searchEngine.searchLink}$inputField")
//        } ?: run {
//          focusManager.clearFocus()
//          keyboardController?.hide()
//          // 如果引擎列表为空，这边不提示，而是直接判断当前的的内容是否符合搜索引擎的，如果符合，直接跳转到引擎首页
//          viewModel.checkAndSearchUI(inputField) {
//            onBlur()
//          }
//        }
//      }
//    })
//  )
//}
//
//@Composable
//fun CustomTextField(
//  value: TextFieldValue,
//  onValueChange: (TextFieldValue) -> Unit,
//  modifier: Modifier = Modifier,
//  label: @Composable (() -> Unit)? = null,
//  leadingIcon: @Composable (() -> Unit)? = null,
//  trailingIcon: @Composable (() -> Unit)? = null,
//  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
//  keyboardActions: KeyboardActions = KeyboardActions.Default,
//  spacerWidth: Dp = 10.dp,
//) {
//  val focusRequester = remember { FocusRequester() }
//  val focusManager = LocalFocusManager.current
//  val showIme = LocalWindowsImeVisible.current
//  val scope = rememberCoroutineScope()
//
//  LaunchedEffect(focusRequester) {
//    delay(100)
//    focusRequester.requestFocus()
//  }
//
//  BasicTextField(
//    value = value,
//    onValueChange = { onValueChange(it) },
//    modifier = modifier.focusRequester(focusRequester),
//    maxLines = 1,
//    singleLine = true,
//    textStyle = TextStyle.Default.copy(
//      fontSize = dimenTextFieldFontSize, color = MaterialTheme.colorScheme.onSecondaryContainer
//    ),
//    keyboardOptions = keyboardOptions,
//    keyboardActions = keyboardActions,
//  ) { innerTextField ->
//    Box(
//      modifier = Modifier.fillMaxSize().pointerInput(focusManager) {
//        awaitEachGesture { // 在clickable中，会被栏拦截事件，所以只能这么写了，单次是 awaitPointerEventScope
//          awaitPointerEvent(PointerEventPass.Initial)
//          if (!showIme.value) {
//            scope.launch { // 键盘手动隐藏后，再次点击不显示问题
//              focusManager.clearFocus()
//              focusRequester.requestFocus()
//            }
//          }
//        }
//      }, contentAlignment = Alignment.CenterStart
//    ) {
//      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
//        Spacer(modifier = Modifier.width(spacerWidth))
//        if (leadingIcon != null) {
//          leadingIcon()
//          Spacer(modifier = Modifier.width(spacerWidth))
//        }
//        Box(
//          modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart
//        ) {
//          innerTextField()
//          if (label != null && value.isEmpty()) label() // 如果内容是空的才显示
//        }
//        if (trailingIcon != null) {
//          Spacer(modifier = Modifier.width(spacerWidth))
//          trailingIcon()
//        }
//        Spacer(modifier = Modifier.width(spacerWidth))
//      }
//    }
//  }
//}

/**
 * 输入搜索内容后，显示的搜索建议
 */
@Composable
private fun SearchSuggestion(
  searchText: String,
  modifier: Modifier = Modifier,
  onClose: () -> Unit,
  onOpenApp: (SearchInject) -> Unit,
  onSubmit: (String) -> Unit,
) {
  val viewModel = LocalBrowserViewModel.current
  LazyColumn(
    modifier = modifier.fillMaxSize().padding(horizontal = dimenPageHorizontal)
  ) {
    searchLocalItems(viewModel, searchText, openApp = onOpenApp)
    searchEngineItems(viewModel, searchText, onSearch = onSubmit)
  }
}

/**
 * 本地资源
 */
private fun LazyListScope.searchLocalItems(
  viewModel: BrowserViewModel, searchText: String, openApp: (SearchInject) -> Unit
) {
  val injectList = viewModel.searchInjectList
  if (injectList.isEmpty() && viewModel.filterShowEngines.isNotEmpty()) return // 如果本地资源为空，但是搜索引擎不为空，不需要显示这个内容
  /// 标题
  item {
    Text(
      text = BrowserI18nResource.browser_search_local(),
      color = MaterialTheme.colorScheme.outline,
      modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
      style = MaterialTheme.typography.titleMedium,
    )
  }

  if (injectList.isEmpty()) {
    item {
      ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = {
          Text(text = BrowserI18nResource.browser_search_noFound())
        },
        leadingContent = {
          Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(40.dp)
          )
        },
      )
    }
    return
  }
  itemsIndexed(injectList) { index, searchInject ->
    if (index > 0) HorizontalDivider()
    ListItem(headlineContent = {
      Text(text = searchInject.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }, modifier = Modifier.clickable { openApp(searchInject) }, supportingContent = {
      Text(text = searchText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }, leadingContent = {
      Image(searchInject.iconPainter(), contentDescription = searchInject.name)
    })
  }
}

private fun LazyListScope.searchEngineItems(
  viewModel: BrowserViewModel, searchText: String, onSearch: (String) -> Unit
) {
  val list = viewModel.filterShowEngines
  if (list.isEmpty()) return // 如果空的直接不显示
  // 标题
  item {
    Text(
      text = BrowserI18nResource.browser_search_engine(),
      color = MaterialTheme.colorScheme.outline,
      modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
      style = MaterialTheme.typography.titleMedium,
    )
  }

  itemsIndexed(list) { index, searchEngine ->
    key(searchEngine.host) {
      if (index > 0) HorizontalDivider()
      ListItem(headlineContent = {
        Text(text = searchEngine.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
      },
        modifier = Modifier.clickable { onSearch(searchEngine.searchLink.format(searchText)) },
        supportingContent = {
          Text(text = searchText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
          AppIcon(
            icon = searchEngine.iconLink,
            modifier = Modifier.size(56.dp),
            iconFetchHook = viewModel.browserNMM.imageFetchHook
          )
        })
    }
  }
}
