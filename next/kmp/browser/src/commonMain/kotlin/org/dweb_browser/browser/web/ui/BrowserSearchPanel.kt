package org.dweb_browser.browser.web.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.delay
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserPage
import org.dweb_browser.browser.web.ui.search.SearchSuggestion
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
      enter = remember { fadeIn(enterAnimationSpec()) + slideInVertically(enterAnimationSpec()) { it } },
      exit = remember { fadeOut(exitAnimationSpec()) + slideOutVertically(exitAnimationSpec()) { it } },
    ) {
      val searchPage = showSearchPage ?: return@AnimatedVisibility
      val focusManager = LocalFocusManager.current
      val focusRequester = remember { FocusRequester() }
      val hide = {
        focusManager.clearFocus()
        viewModel.hideAllPanel()
      }
      /// 返回关闭搜索
      val win = LocalWindowController.current
      win.navigation.GoBackHandler {
        hide()
      }
      val searchTextState = rememberTextFieldState("")
      remember(viewModel.searchKeyWord, searchPage.url) {
        searchTextState.setTextAndSelectAll(viewModel.searchKeyWord ?: searchPage.url)
      }
      val searchText = searchTextState.text.toString()
      val searchBarColors = SearchBarDefaults.colors()
      val searchFieldColors = SearchBarDefaults.inputFieldColors()
      var suggestionActions by remember { mutableStateOf<List<() -> Unit>>(emptyList()) }
      Column(
        modifier = Modifier.fillMaxSize().background(searchBarColors.containerColor),
      ) {
        /// 搜索框
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
            if (searchTextState.selection.start != 0 || searchTextState.selection.end != searchTextState.text.length) {
              searchTextState.setTextAndSelectAll(searchText)
            }
          }
        }
        val interactionSource = remember { MutableInteractionSource() }
        /**
         * TODO 完成自动完成的功能
         * 1. 搜索历史
         * 2. 浏览器访问记录，尝试匹配 title 与 url，并提供 icon、title、url等基本信息进行显示
         */
        BasicTextField(state = searchTextState,
          modifier = Modifier.fillMaxWidth(
          ).heightIn(min = SearchBarDefaults.InputFieldHeight).focusRequester(focusRequester),
          lineLimits = TextFieldLineLimits.SingleLine,
          textStyle = LocalTextStyle.current.copy(color = searchFieldColors.focusedTextColor),
          cursorBrush = SolidColor(searchFieldColors.cursorColor),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          onKeyboardAction = {
            focusManager.clearFocus()
            suggestionActions.firstOrNull()?.invoke()
//            focusRequester.freeFocus()
//            hide()
//            viewModel.doIOSearchUrl(searchText)
          },
          interactionSource = interactionSource,
          decorator = { innerTextField ->
            TextFieldDefaults.DecorationBox(
              value = searchText,
              innerTextField = innerTextField,
              enabled = true,
              singleLine = true,
              visualTransformation = VisualTransformation.None,
              interactionSource = interactionSource,
              leadingIcon = {
                // leadingIcon
                IconButton(onClick = hide) {
                  Icon(Icons.Rounded.KeyboardArrowDown, "close search panel")
                }
              },
              trailingIcon = {
                // trailingIcon 清除文本的按钮
                IconButton(onClick = {
                  // 清空文本之后再次点击需要还原文本内容并对输入框失焦
                  if (searchText.isEmpty()) {
                    searchTextState.setTextAndPlaceCursorAtEnd(searchPage.url)
                    hide()
                  } else {
                    searchTextState.clearText()
                  }
                }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear Input Text")
                }
              },
            )
          })

        HorizontalDivider(color = searchBarColors.dividerColor)
        /// 面板内容
        SearchSuggestion(
          searchTextState = searchTextState,
          modifier = Modifier.fillMaxSize(),
          onClose = hide,
          onSuggestionActions = { suggestionActions = it }
        )
      }
    }
    return showSearchPage != null
  }
}

