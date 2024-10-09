package org.dweb_browser.browser.web.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.twotone.Public
import androidx.compose.material.icons.twotone.TravelExplore
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.search.SearchInject
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.couldBeUrlStart
import org.dweb_browser.browser.web.model.page.BrowserPage
import org.dweb_browser.browser.web.ui.common.BrowserTopBar
import org.dweb_browser.helper.format
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isDesktop
import org.dweb_browser.sys.window.core.LocalWindowController
import org.dweb_browser.sys.window.helper.LocalWindowsImeVisible

class BrowserSearchPanel(val viewModel: BrowserViewModel) {
  private var showSearchPage by mutableStateOf<BrowserPage?>(null) // 用于显示搜索框的
  val showPanel get() = showSearchPage != null

  fun hideSearchPanel() {
    showSearchPage = null
  }

  fun showSearchPanel(page: BrowserPage) {
    showSearchPage = page
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun Render(modifier: Modifier = Modifier): Boolean {
    val viewModel = LocalBrowserViewModel.current
    AnimatedVisibility(
      visible = showSearchPage != null,
      modifier = modifier,
      enter = remember { slideInVertically(enterAnimationSpec()) { it } },
      exit = remember { slideOutVertically(exitAnimationSpec()) { it } },
    ) {
      val searchPage = showSearchPage ?: return@AnimatedVisibility
      val focusManager = LocalFocusManager.current
      val hide = {
        focusManager.clearFocus()
        viewModel.hideAllPanel()
      }
      /// 返回关闭搜索
      val win = LocalWindowController.current
      win.navigation.GoBackHandler {
        hide()
      }
      var searchTextField by remember(viewModel.searchKeyWord, searchPage.url) {
        val text = viewModel.searchKeyWord ?: searchPage.url
        mutableStateOf(
          TextFieldValue(text = text, selection = TextRange(0, text.length))
        )
      }
      val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
      Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                  hide()
                  viewModel.doIOSearchUrl(url)
                })
            }
          }

          /// 底部的输入框
          Box(
            Modifier.fillMaxWidth().height(dimenBottomHeight)
              .padding(horizontal = dimenPageHorizontal),
            contentAlignment = Alignment.Center,
          ) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(focusRequester) {
              delay(100)
              // MacOS桌面端会出现窗口失焦，导致无法直接输入
              if (IPureViewController.isDesktop) {
                win.focus()
              }
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

            val couldBeUrlStart = searchTextField.text.couldBeUrlStart()

            /**
             * TODO 完成自动完成的功能
             * 1. 搜索历史
             * 2. 浏览器访问记录，尝试匹配 title 与 url，并提供 icon、title、url等基本信息进行显示
             */
            BasicTextField(value = searchTextField,
              onValueChange = { searchTextField = it },
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).border(
                1.dp, MaterialTheme.colorScheme.primary, browserShape
              ).background(MaterialTheme.colorScheme.outlineVariant, browserShape)
                .focusRequester(focusRequester),
              singleLine = true,
              maxLines = 1,
              textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSecondaryContainer
              ),
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Search,
              ),
              keyboardActions = KeyboardActions(
                onSearch = {
                  hide()
                  viewModel.doIOSearchUrl(searchTextField.text.trim().trim('\u200B').trim())
                },
              ),
              decorationBox = { innerTextField ->
                Row(
                  modifier = Modifier.fillMaxSize(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    modifier = Modifier.fillMaxHeight().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    // leadingIcon
                    Box(
                      Modifier.fillMaxHeight().aspectRatio(1f), contentAlignment = Alignment.Center
                    ) {
                      if (couldBeUrlStart) {
                        Icon(Icons.TwoTone.Public, "visit network")
                      } else {
                        Icon(Icons.TwoTone.TravelExplore, "search network")
                      }
                    }
                    Box(
                      modifier = Modifier.weight(1f),
                      contentAlignment = Alignment.CenterStart,
                    ) {
                      innerTextField()
                      // Placeholder
                      if (searchTextField.text.isEmpty()) {
                        Text(
                          text = BrowserI18nResource.browser_search_hint(),
                          fontSize = dimenTextFieldFontSize,
                          maxLines = 1,
                          modifier = Modifier.alpha(0.5f)
                        )
                      }
                    }
                  }

                  // trailingIcon 清除文本的按钮
                  IconButton(onClick = {
                    // 清空文本之后再次点击需要还原文本内容并对输入框失焦
                    if (searchTextField.text.isEmpty()) {
                      searchTextField = TextFieldValue(searchPage.url)
                      hide()
                    } else {
                      searchTextField = TextFieldValue("")
                    }
                  }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Input Text")
                  }
                }
              })
          }
        }
      }
    }
    return showSearchPage != null
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
  viewModel: BrowserViewModel, searchText: String, openApp: (SearchInject) -> Unit,
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
  viewModel: BrowserViewModel, searchText: String, onSearch: (String) -> Unit,
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
