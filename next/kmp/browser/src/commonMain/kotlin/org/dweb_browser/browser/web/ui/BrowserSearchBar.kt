package org.dweb_browser.browser.web.ui


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.util.isSystemUrl
import org.dweb_browser.browser.web.data.page.BrowserPage
import org.dweb_browser.browser.web.data.page.BrowserWebPage
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalShowIme
import org.dweb_browser.browser.web.model.LocalShowSearchView
import org.dweb_browser.browser.web.model.parseInputText
import org.dweb_browser.dwebview.rememberLoadingProgress

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserSearchBar(viewModel: BrowserViewModel) {
  val localShowIme = LocalShowIme.current

  /*LaunchedEffect(pagerStateNavigator.settledPage) { // 为了修复隐藏搜索框后，重新加载时重新显示的问题，会显示第一页
    delay(100)
    pagerStateNavigator.scrollToPage(pagerStateNavigator.settledPage)
  }*/
  val localFocus = LocalFocusManager.current
  LaunchedEffect(Unit) {
    viewModel.showMore
    if (!localShowIme.value && !viewModel.showSearchEngine) {
      localFocus.clearFocus()
    }
  }

  HorizontalPager(
    modifier = Modifier,
    state = viewModel.pagerStates.searchBar,
    pageSpacing = 0.dp,
    userScrollEnabled = true,
    reverseLayout = false,
    contentPadding = PaddingValues(horizontal = dimenHorizontalPagerHorizontal),
    beyondBoundsPageCount = 5,
    pageContent = { currentPage ->
      SearchBox(viewModel.getPageOrNull(currentPage)!!)
    },
  )
}


@Composable
private fun SearchBox(page: BrowserPage) {
  var showSearchView by LocalShowSearchView.current
  val searchHint = BrowserI18nResource.browser_search_hint()

  Box(modifier = Modifier.padding(
    horizontal = dimenSearchHorizontalAlign, vertical = dimenSearchVerticalAlign
  ).fillMaxWidth()
    .shadow(
      elevation = dimenShadowElevation,
      shape = RoundedCornerShape(dimenSearchRoundedCornerShape)
    )
    .height(dimenSearchHeight)
    .clip(RoundedCornerShape(dimenSearchRoundedCornerShape))
    .background(MaterialTheme.colorScheme.surface)
    .clickable { showSearchView = true }) {
    if (page is BrowserWebPage) {
      ShowLinearProgressIndicator(page)
    }
    val inputText = page.url
    val (title, align, icon) = if (inputText.isEmpty() || inputText.isSystemUrl()) {
      Triple(searchHint, TextAlign.Start, Icons.Default.Search)
    } else {
      Triple(parseInputText(inputText), TextAlign.Center, Icons.Default.FormatSize)
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).align(Alignment.Center),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (page.icon != null) {

      }
      Icon(icon, contentDescription = "Search")
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = title,
        textAlign = align,
        fontSize = dimenTextFieldFontSize,
        maxLines = 1,
        modifier = Modifier.weight(1f)
      )
    }
  }
}

/**
 * 用于显示 WebView 加载进度
 */
@Composable
private fun BoxScope.ShowLinearProgressIndicator(page: BrowserWebPage) {
  when (val loadingProgress = page.webView.rememberLoadingProgress()) {
    0f, 1f -> {}
    else -> {
      LinearProgressIndicator(
        progress = { loadingProgress },
        modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}
