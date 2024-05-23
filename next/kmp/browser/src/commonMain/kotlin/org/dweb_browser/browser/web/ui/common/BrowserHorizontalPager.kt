package org.dweb_browser.browser.web.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
expect fun BoxWithConstraintsScope.BrowserHorizontalPager(
  state: PagerState,
  modifier: Modifier = Modifier,
  pageContent: @Composable() (PagerScope.(Int) -> Unit)
)