package org.dweb_browser.browser.web.ui


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.util.isDeepLink
import org.dweb_browser.browser.web.data.page.BrowserHomePage
import org.dweb_browser.browser.web.data.page.BrowserPage
import org.dweb_browser.browser.web.data.page.BrowserWebPage
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.LocalShowIme
import org.dweb_browser.browser.web.model.LocalShowSearchView
import org.dweb_browser.browser.web.model.searchBarTextTransformer
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
      SearchBox(viewModel.getPage(currentPage))
    },
  )
}


@Composable
private fun SearchBox(page: BrowserPage) {
  var showSearchView by LocalShowSearchView.current
  val viewModel = LocalBrowserViewModel.current
  val scope = viewModel.ioScope

  Box(modifier = Modifier.padding(
    horizontal = dimenSearchHorizontalAlign, vertical = dimenSearchVerticalAlign
  ).fillMaxWidth().shadow(
    elevation = dimenShadowElevation, shape = RoundedCornerShape(dimenSearchRoundedCornerShape)
  ).height(dimenSearchHeight).clip(RoundedCornerShape(dimenSearchRoundedCornerShape))
    .background(MaterialTheme.colorScheme.surface).clickable { showSearchView = true }) {
    if (page is BrowserWebPage) {
      ShowLinearProgressIndicator(page)
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).align(Alignment.Center),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      if (page is BrowserHomePage) {
        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.alpha(0.5f))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = BrowserI18nResource.browser_search_hint(),
          textAlign = TextAlign.Start,
          maxLines = 1,
          modifier = Modifier.weight(1f).alpha(0.5f)
        )
      } else {
        val pageTitle = page.title
        val pageIcon = page.icon
        val pageUrl = page.url
        Icon(
          painter = if (pageUrl.isDeepLink()) BrowserDrawResource.Logo.painter()
          else (pageIcon ?: BrowserDrawResource.Web.painter()),
          contentDescription = pageTitle,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = searchBarTextTransformer(pageUrl),
          textAlign = TextAlign.Center,
          maxLines = 1,
          modifier = Modifier.weight(1f)
        )
      }

      if (page is BrowserWebPage) {
        Spacer(modifier = Modifier.width(5.dp))
        IconButton({
          scope.launch {
            page.webView.reload()
          }
        }) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh")
        }
      }
    }
  }
}

/**
 * 用于显示 WebView 加载进度
 */
@Composable
private fun BoxScope.ShowLinearProgressIndicator(page: BrowserWebPage) {
  val loadingProgress = page.webView.rememberLoadingProgress()
  AnimatedVisibility(
    loadingProgress > 0 && loadingProgress < 1,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = Modifier.zIndex(2f)
  ) {
    LinearProgressIndicator(
      progress = { loadingProgress },
      modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
      color = MaterialTheme.colorScheme.primary,
    )
  }
}
