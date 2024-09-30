package org.dweb_browser.browser.web.ui


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Filter1
import androidx.compose.material.icons.rounded.Filter2
import androidx.compose.material.icons.rounded.Filter3
import androidx.compose.material.icons.rounded.Filter4
import androidx.compose.material.icons.rounded.Filter5
import androidx.compose.material.icons.rounded.Filter6
import androidx.compose.material.icons.rounded.Filter7
import androidx.compose.material.icons.rounded.Filter8
import androidx.compose.material.icons.rounded.Filter9
import androidx.compose.material.icons.rounded.Filter9Plus
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.LocalShowIme
import org.dweb_browser.helper.compose.ScalePopupPlaceholder
import org.dweb_browser.helper.compose.hoverCursor

@Composable
fun BrowserBottomBar(scale: Float, modifier: Modifier) {
  val viewModel = LocalBrowserViewModel.current
  val uiScope = rememberCoroutineScope()
  val localShowIme = LocalShowIme.current

  val localFocus = LocalFocusManager.current
  LaunchedEffect(Unit) {
    if (!localShowIme.value) {
      localFocus.clearFocus()
    }
  }
  Row(
    modifier,
    horizontalArrangement = Arrangement.SpaceAround,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 新增页面
    IconButton({
      uiScope.launch {
        // 添加新页面到当前页面到后面
        viewModel.addNewPageUI {
          addIndex = viewModel.focusedPageIndex + 1
        }
      }
    }, Modifier.hoverCursor()) {
      Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = "Add New Page",
      )
    }

    BoxWithConstraints(modifier = Modifier.weight(1f)) {
      viewModel.isTabFixedSize = maxWidth >= 280.dp
      if (viewModel.isTabFixedSize) {
        // 如果宽度过大，标签使用固定大小
        LazyRow(
          state = viewModel.pagerStates.tabsBarLazyState,
          contentPadding = PaddingValues(8.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          items(
            viewModel.pageSize,
            key = { pageIndex -> viewModel.getPage(pageIndex).hashCode() }) { pageIndex ->
            PageTabWithToolTip(
              viewModel.getPage(pageIndex),
              Modifier.requiredSizeIn(minWidth = 180.dp, maxWidth = 220.dp)
            )
          }
        }
      } else {
        viewModel.pagerStates.SearchBarEffect()
        HorizontalPager(
          state = viewModel.pagerStates.tabsBarPager,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(8.dp),
          pageSize = PageSize.Fill,
          // beyondViewportPageCount = Int.MAX_VALUE,
          userScrollEnabled = true,
          pageSpacing = 4.dp,
          key = { pageIndex -> viewModel.getPage(pageIndex).hashCode() },
          pageContent = { pageIndex ->
            PageTabWithToolTip(
              viewModel.getPage(pageIndex),
              Modifier.requiredSizeIn(minWidth = 180.dp)
            )
          },
        )
      }
    }

    // 多窗口预览界面
    IconButton(onClick = {
      uiScope.launch {
        viewModel.focusedPage?.captureViewInBackground("for preview")
        viewModel.previewPanel.toggleShowPreviewUI(PreviewPanelVisibleState.DisplayGrid)
      }
    }, Modifier.hoverCursor()) {
      Icon(
        imageVector = getMultiImageVector(viewModel.pageSize),
        contentDescription = "Open Preview Panel",
      )
    }

    // 功能列表
    IconButton(onClick = { viewModel.showMore = true }, Modifier.hoverCursor()) {
      ScalePopupPlaceholder(scale) {
        BrowserMenuPanel(scale)
      }
      Icon(
        imageVector = Icons.Rounded.MoreVert,
        contentDescription = "Open Menu Panel",
      )
    }
  }
}

internal fun getMultiImageVector(size: Int) = when (size) {
  1 -> Icons.Rounded.Filter1
  2 -> Icons.Rounded.Filter2
  3 -> Icons.Rounded.Filter3
  4 -> Icons.Rounded.Filter4
  5 -> Icons.Rounded.Filter5
  6 -> Icons.Rounded.Filter6
  7 -> Icons.Rounded.Filter7
  8 -> Icons.Rounded.Filter8
  9 -> Icons.Rounded.Filter9
  else -> Icons.Rounded.Filter9Plus
}
