package org.dweb_browser.browser.web.ui


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.LocalShowIme
import org.dweb_browser.browser.web.model.page.BrowserHomePage
import org.dweb_browser.browser.web.model.page.BrowserPage
import org.dweb_browser.browser.web.model.page.BrowserWebPage
import org.dweb_browser.browser.web.model.pageUrlTransformer
import org.dweb_browser.dwebview.rememberLoadingProgress
import org.dweb_browser.helper.isDwebDeepLink

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
      Box(Modifier.padding(horizontal = dimenHorizontalPagerHorizontal / 2)) {
        SearchBox(viewModel.getPage(currentPage))
      }
    },
  )
}


enum class SearchBoxTheme {
  Shadow, Border, ;
}

/// 用于搜索框的外部风格化，提供了阴影风格和边框风格
internal fun Modifier.searchBoxStyle(boxTheme: SearchBoxTheme) = composed {
  height(dimenSearchHeight).then(
    when (boxTheme) {
      SearchBoxTheme.Shadow -> Modifier.shadow(
        elevation = dimenShadowElevation, shape = RoundedCornerShape(dimenSearchRoundedCornerShape)
      )

      SearchBoxTheme.Border -> Modifier.border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline,
        RoundedCornerShape(dimenSearchRoundedCornerShape)
      )
    }
  ).background(MaterialTheme.colorScheme.surface)
    .clip(RoundedCornerShape(dimenSearchRoundedCornerShape))
}

/// 用于搜索框内部的基础样式，提供了基本的边距控制
internal fun Modifier.searchInnerStyle(start: Boolean = true, end: Boolean = true) = padding(
  start = if (start) dimenSearchHorizontalAlign else 0.dp,
  end = if (end) dimenSearchHorizontalAlign else 0.dp,
  top = dimenSearchVerticalAlign,
  bottom = dimenSearchVerticalAlign,
)

@Composable
private fun SearchBox(page: BrowserPage) {
  val viewModel = LocalBrowserViewModel.current
  val scope = viewModel.ioScope

  Box(modifier = Modifier.fillMaxWidth().searchBoxStyle(SearchBoxTheme.Shadow).clickable {
    viewModel.showSearch = page
  }) {
    if (page is BrowserWebPage) {
      ShowLinearProgressIndicator(page)
    }

    Row(
      modifier = Modifier.fillMaxSize(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      val emptyTheme = page is BrowserHomePage
      /// 左边的图标，正方形大小，图标剧中
      Box(Modifier.size(dimenSearchHeight), contentAlignment = Alignment.Center) {
        if (emptyTheme) {
          Icon(
            Icons.Default.Search,
            contentDescription = "Search",
            modifier = Modifier.alpha(0.5f).wrapContentWidth()
          )
        } else {
          val pageTitle = page.title
          val pageIcon = page.icon
          val pageUrl = page.url
          val isDwebDeeplink = remember(pageUrl) { pageUrl.isDwebDeepLink() }
          Image(
            painter = if (isDwebDeeplink) BrowserDrawResource.Logo.painter()
            else (pageIcon ?: BrowserDrawResource.Web.painter()),
            contentDescription = pageTitle,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      val textModifier = Modifier.weight(1f).searchInnerStyle(start = false, end = false)
      if (emptyTheme) {
        Text(
          text = BrowserI18nResource.browser_search_hint(),
          textAlign = TextAlign.Start,
          maxLines = 2,
          modifier = textModifier.alpha(0.5f),
          fontSize = dimenTextFieldFontSize,
          overflow = TextOverflow.Ellipsis,
        )
      } else {
        val pageUrl = page.url
        Text(
          text = remember(pageUrl) { pageUrlTransformer(pageUrl) },
          textAlign = TextAlign.Center,
          maxLines = 2,
          modifier = textModifier,
          fontSize = dimenTextFieldFontSize,
          overflow = TextOverflow.Ellipsis,
        )
      }

      /// 右边的图标，正方形大小，图标剧中
      if (page is BrowserWebPage) {
        IconButton({
          scope.launch {
            page.webView.reload()
          }
        }, modifier = Modifier.wrapContentWidth()) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh")
        }
      } else {
        Spacer(modifier = Modifier.width(dimenSearchHeight))
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
