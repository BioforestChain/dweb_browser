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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.LocalShowIme
import org.dweb_browser.browser.web.model.page.BrowserHomePage
import org.dweb_browser.browser.web.model.page.BrowserPage
import org.dweb_browser.browser.web.model.page.BrowserWebPage
import org.dweb_browser.browser.web.model.pageUrlTransformer
import org.dweb_browser.browser.web.ui.common.BrowserHorizontalPager
import org.dweb_browser.dwebview.rememberLoadingProgress
import org.dweb_browser.helper.isDwebDeepLink

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserSearchBar(modifier: Modifier) {
  val viewModel = LocalBrowserViewModel.current
  val uiScope = rememberCoroutineScope()
  val localShowIme = LocalShowIme.current

  val localFocus = LocalFocusManager.current
  LaunchedEffect(Unit) {
    if (!localShowIme.value) {
      localFocus.clearFocus()
    }
  }
  val onceItemSize = dimenSearchHeight
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
    }) {
      Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = "Add",
        modifier = Modifier.size(onceItemSize).padding(4.dp)
      )
    }

    BoxWithConstraints(modifier = Modifier.weight(1f)) {
      if (maxWidth > 20.dp) { // 由于宽度压缩太小，导致HorizontalPager空间不足，width<0,引起的crash
        BrowserHorizontalPager(
          state = viewModel.pagerStates.searchBar,
          modifier = Modifier.fillMaxWidth(),
          pageContent = { currentPage ->
            SearchBox(viewModel.getPage(currentPage))
          },
        )
      }
    }

    // 多窗口预览界面
    IconButton(modifier = Modifier.size(onceItemSize), onClick = {
      uiScope.launch {
        viewModel.focusedPage?.captureView("for preview")
        viewModel.toggleShowPreviewUI(BrowserViewModel.PreviewPanelVisibleState.DisplayGrid)
      }
    }) {
      Icon(
        imageVector = getMultiImageVector(viewModel.pageSize),
        contentDescription = "Open Preview Panel",
        modifier = Modifier.size(onceItemSize).padding(6.dp)
      )
    }

    // 功能列表
    IconButton(modifier = Modifier.size(onceItemSize), onClick = { viewModel.showMore = true }) {
      BrowserMenuPanel()
      Icon(
        imageVector = Icons.Rounded.MoreVert,
        contentDescription = "Open Menu Panel",
        modifier = Modifier.size(onceItemSize).padding(4.dp)
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

enum class SearchBoxTheme {
  Shadow, Border, Focused, Unfocused,
  ;
}

/// 用于搜索框的外部风格化，提供了阴影风格和边框风格
internal fun Modifier.searchBoxStyle(boxTheme: SearchBoxTheme) = composed {
  height(dimenSearchHeight).then(
    when (boxTheme) {
      SearchBoxTheme.Shadow, SearchBoxTheme.Focused -> Modifier.shadow(
        elevation = dimenShadowElevation,
        shape = RoundedCornerShape(dimenSearchRoundedCornerShape),
        ambientColor = LocalContentColor.current,
        spotColor = LocalContentColor.current,
      ).background(MaterialTheme.colorScheme.background)

      SearchBoxTheme.Border -> Modifier.border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline,
        RoundedCornerShape(dimenSearchRoundedCornerShape)
      ).background(MaterialTheme.colorScheme.background)

      SearchBoxTheme.Unfocused -> Modifier.clip(RoundedCornerShape(dimenSearchRoundedCornerShape))
        .background(MaterialTheme.colorScheme.outlineVariant)
    }
  )
}

/// 用于搜索框内部的基础样式，提供了基本的边距控制
internal fun Modifier.searchInnerStyle(start: Boolean = true, end: Boolean = true) = padding(
  start = if (start) dimenSearchInnerHorizontal else 0.dp,
  end = if (end) dimenSearchInnerHorizontal else 0.dp,
  top = dimenSearchInnerVertical,
  bottom = dimenSearchInnerVertical,
)

@Composable
private fun SearchBox(page: BrowserPage) {
  val viewModel = LocalBrowserViewModel.current
  val scope = viewModel.lifecycleScope
  val isFocus =
    viewModel.isFillPageSize || page == viewModel.focusedPage // 确认是否是聚焦的页面，如果Page模式是fill直接聚焦，另外就是如果page是当前页，需要突出显示

  Box(
    modifier = Modifier.fillMaxWidth()
      .searchBoxStyle(if (isFocus) SearchBoxTheme.Focused else SearchBoxTheme.Unfocused)
      .clickable {
        // 增加判断，如果当前点击的是当前界面，那么就显示搜索框；如果不是，那么进行focus操作
        scope.launch {
          if (page == viewModel.focusedPage) {
            viewModel.showSearchPage = page
          } else {
            viewModel.focusPageUI(page)
          }
        }
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
        val pageTitle = page.title
        val pageIcon = page.icon
        val pageUrl = page.url
        val painter: Painter
        val colorFilter: ColorFilter?
        when (remember(pageUrl) { pageUrl.isDwebDeepLink() }) {
          true -> {
            painter = BrowserDrawResource.Logo.painter()
            colorFilter = BrowserDrawResource.Logo.getContentColorFilter()
          }

          else -> when (pageIcon) {
            null -> {
              painter = BrowserDrawResource.Web.painter()
              colorFilter = BrowserDrawResource.Web.getContentColorFilter()
            }

            else -> {
              painter = pageIcon
              colorFilter = page.iconColorFilter
            }
          }
        }
        Image(
          painter = painter,
          colorFilter = colorFilter,
          contentDescription = pageTitle,
          modifier = Modifier.size(24.dp),
        )
      }

      val textModifier = Modifier.weight(1f).searchInnerStyle(start = false, end = false)
      val pageUrl = page.url
      Text(
        text = remember(pageUrl) { pageUrlTransformer(pageUrl) },
        textAlign = TextAlign.Center,
        maxLines = 2,
        modifier = textModifier,
        fontSize = dimenTextFieldFontSize,
        overflow = TextOverflow.Ellipsis,
      )
      /// 右边的图标，正方形大小，图标剧中
      if (page is BrowserWebPage) {
        IconButton(
          onClick = { scope.launch { page.webView.reload() } },
          modifier = Modifier.wrapContentWidth()
        ) {
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
