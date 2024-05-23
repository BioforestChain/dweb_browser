package org.dweb_browser.browser.web.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.web.model.LocalBrowserViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun BoxWithConstraintsScope.BrowserHorizontalPager(
  state: PagerState, modifier: Modifier, pageContent: @Composable() (PagerScope.(Int) -> Unit)
) {
  val viewModel = LocalBrowserViewModel.current
  LaunchedEffect(Unit) { viewModel.isFillPageSize = false /* 桌面端使用Fixed */ }
  HorizontalPager(
    state = state,
    modifier = modifier,
    pageSize = PageSize.Fixed(220.dp),
    contentPadding = PaddingValues(10.dp),
    pageSpacing = 5.dp,
    userScrollEnabled = true,
    reverseLayout = false,
    pageContent = pageContent
  )
}