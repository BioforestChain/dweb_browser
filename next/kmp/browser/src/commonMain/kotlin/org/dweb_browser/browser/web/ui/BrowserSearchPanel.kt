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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.CommonTextField
import org.dweb_browser.browser.search.SearchInject
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.ui.common.BrowserTopBar
import org.dweb_browser.helper.format
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.LocalWindowsImeVisible

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
      viewModel.showSearch?.searchKeyWord = null // 关闭之前，先把这个字段内容情况，避免下次显示关键字仍然为之前的
      viewModel.showSearch = null
    }
    /// 返回关闭搜索
    LocalWindowController.current.GoBackHandler {
      hide()
    }
    val uiScope = rememberCoroutineScope()
    var searchTextField by remember(searchPage.searchKeyWord, searchPage.url) {
      val text = searchPage.searchKeyWord ?: searchPage.url
      mutableStateOf(
        TextFieldValue(text = text, selection = TextRange(0, text.length))
      )
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
      modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      contentWindowInsets = WindowInsets(0),
      topBar = {
        BrowserTopBar(
          title = BrowserI18nResource.browser_search_title(),
          navigationIcon = { /// 左上角导航按钮
            IconButton(onClick = hide) {
              Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close Search Panel"
              )
            }
          },
          actions = { /// 右上角功能按钮
            // TODO 提供搜索的设置
          },
          scrollBehavior = scrollBehavior
        )
      },
    ) { innerPadding ->
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
        Box(Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = dimenPageHorizontal)) {
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
          CommonTextField(
            value = searchTextField,
            onValueChange = { searchTextField = it },
            modifier = Modifier.fillMaxWidth().searchBoxStyle(SearchBoxTheme.Border)
              .focusRequester(focusRequester),
            singleLine = true,
            maxLines = 1,
            textStyle = TextStyle.Default.copy(
              fontSize = dimenTextFieldFontSize,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            onKeyboardSearch = {
              uiScope.launch {
                viewModel.doSearchUI(searchTextField.text.trim().trim('\u200B').trim())
                hide()
              }
            },
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
              IconButton(onClick = { searchTextField = TextFieldValue("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear Input Text")
              }
            }
          }
        }
      }
    }

  }
}

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
  LaunchedEffect(searchText) { viewModel.getInjectList(searchText) }
  LazyColumn(
    modifier = modifier.fillMaxSize().padding(horizontal = dimenPageHorizontal)
  ) {
    searchLocalItems(viewModel, searchText, openApp = { onOpenApp(it); onClose() })
    searchEngineItems(viewModel, searchText, onSearch = { onSubmit(it); onClose() })
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
        Text(text = searchEngine.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }, modifier = Modifier.clickable {
        onSearch(searchEngine.searchLinks.first().format(searchText))
      }, supportingContent = {
        Text(text = searchText, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }, leadingContent = {
        Image(
          painter = searchEngine.painter(),
          contentDescription = searchEngine.displayName,
          modifier = Modifier.size(56.dp),
        )
      })
    }
  }
}
