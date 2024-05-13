package org.dweb_browser.browser.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun BoxWithConstraintsScope.CommonHorizontalPager(
  state: PagerState,
  modifier: Modifier,
  pageContent: @Composable() (PagerScope.(Int) -> Unit)
) {
  if (maxWidth > 20.dp) {
//    HorizontalPager(
//      state = state,
//      modifier = modifier,
//      pageSize = PageSize.Fixed(220.dp),
//      contentPadding = PaddingValues(10.dp),
//      pageSpacing = 5.dp,
//      userScrollEnabled = true,
//      reverseLayout = false,
//      pageContent = pageContent
//    )
    HorizontalPager(
      state = state,
      modifier = modifier,
      contentPadding = PaddingValues(10.dp),
      pageSize = PageSize.Fill,
      beyondBoundsPageCount = 5,
      userScrollEnabled = true,
      reverseLayout = false,
      pageSpacing = 5.dp,
      pageContent = pageContent
    )
  }
}