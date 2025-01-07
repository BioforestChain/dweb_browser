package org.dweb_browser.browser.web.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserHomePage
import org.dweb_browser.browser.web.model.page.BrowserPage
import org.dweb_browser.browser.web.model.page.BrowserWebPage
import org.dweb_browser.dwebview.rememberHistoryCanGoBack
import org.dweb_browser.dwebview.rememberHistoryCanGoForward
import org.dweb_browser.dwebview.rememberLoadingProgress
import org.dweb_browser.helper.compose.ComposeWindowFocusOwnerEffect
import org.dweb_browser.helper.compose.ScalePopupContent
import org.dweb_browser.helper.compose.ScalePopupPlaceholder
import org.dweb_browser.helper.compose.hoverCursor
import org.dweb_browser.helper.compose.pointerActions
import org.dweb_browser.helper.isDwebDeepLink
import org.dweb_browser.helper.toWebUrl
import org.dweb_browser.sys.window.render.AppIconContainer

enum class SearchBoxTheme {
  Focused, Unfocused,
  ;
}

/// 用于搜索框的外部风格化，提供了阴影风格和边框风格
internal fun Modifier.pagerTabStyle(boxTheme: SearchBoxTheme) = composed {
  when (boxTheme) {
    SearchBoxTheme.Focused -> shadow(
      elevation = dimenShadowElevation,
      shape = browserShape,
      ambientColor = LocalContentColor.current,
      spotColor = LocalContentColor.current,
    ).background(MaterialTheme.colorScheme.background)

    SearchBoxTheme.Unfocused -> clip(browserShape).background(MaterialTheme.colorScheme.background)
      .alpha(0.5f)
  }
}

@Composable
internal fun PageTabWithToolTip(page: BrowserPage, modifier: Modifier) {
  val viewModel = LocalBrowserViewModel.current
  val uiScope = rememberCoroutineScope()
  when (page) {
    is BrowserWebPage -> {
      var showActions by remember { mutableStateOf(false) }
      Box {
        PagerTab(
          page, modifier = modifier.pointerActions(
            onMenu = {
              showActions = true
            },
            onTap = {
              if (page == viewModel.focusedPage) {
                showActions = true
              } else {
                uiScope.launch {
                  viewModel.focusPageUI(page)
                }
              }
            },
          )
        )
        ScalePopupPlaceholder(page.scale) {
          BrowserWebPageTabActions(
            onDismissRequest = { showActions = false },
            expanded = showActions,
            page = page,
          )
        }
      }
    }

    else -> PagerTab(page, modifier = modifier.pointerActions(onMenu = {
      viewModel.searchPanel.showSearchPanel(page)
    }, onTap = {
      uiScope.launch {
        // 如果当前点击的是当前界面，那么就显示搜索框；如果不是，那么进行focus操作
        if (page == viewModel.focusedPage) {
          viewModel.searchPanel.showSearchPanel(page)
        } else {
          viewModel.focusPageUI(page)
        }
      }
    }))
  }
}

@Composable
private fun BrowserWebPageTabActions(
  onDismissRequest: () -> Unit,
  expanded: Boolean,
  page: BrowserWebPage,
  modifier: Modifier = Modifier,
) {
  fun scopeLaunch(action: suspend () -> Unit) {
    page.webView.lifecycleScope.launch {
      launch {
        delay(150)
        onDismissRequest()
      }
      action()
    }
  }
  ComposeWindowFocusOwnerEffect(expanded, onDismissRequest)
  DropdownMenu(
    modifier = modifier,
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    containerColor = MaterialTheme.colorScheme.background,
    shape = browserShape,
  ) {
    ScalePopupContent {
      Row {
        IconButton(
          { scopeLaunch { page.browserController.viewModel.searchPanel.showSearchPanel(page) } },
        ) {
          Icon(Icons.Rounded.Edit, "open search panel or edit url")
        }
        IconButton(
          { scopeLaunch { page.webView.goBack() } },
          enabled = page.webView.rememberHistoryCanGoBack()
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, "go back")
        }
        IconButton(
          { scopeLaunch { page.webView.historyGoForward() } },
          enabled = page.webView.rememberHistoryCanGoForward()
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, "go forward")
        }
        IconButton(
          { scopeLaunch { page.webView.reload() } },
        ) {
          Icon(Icons.Rounded.Refresh, "reload web page")
        }
      }
    }
  }
}

@Composable
private fun PagerTab(page: BrowserPage, modifier: Modifier = Modifier) {
  val viewModel = LocalBrowserViewModel.current
  val scope = viewModel.lifecycleScope
  // 确认是否是聚焦的页面，如果Page模式是fill直接聚焦，另外就是如果page是当前页，需要突出显示
  val isFocused = page == viewModel.focusedPage

  Box(
    modifier = modifier.fillMaxWidth().pagerTabStyle(
      when {
        isFocused -> SearchBoxTheme.Focused
        viewModel.isTabFixedSize -> SearchBoxTheme.Unfocused
        else -> SearchBoxTheme.Focused
      }
    ).hoverCursor(if (isFocused) PointerIcon.Text else PointerIcon.Hand)
  ) {
    if (page is BrowserWebPage) {
      ShowLinearProgressIndicator(page)
    }
    val humanPageUrl = page.url.let { pageUrl -> remember(pageUrl) { pageUrlTransformer(pageUrl) } }
    val pageTitle = page.title
    val endIcon: (@Composable (Modifier) -> Unit)? =
      if (viewModel.pageSize == 1 && page is BrowserHomePage) {
        null
      } else {
        { modifier ->
          IconButton(
            onClick = { scope.launch { viewModel.closePageUI(page) } }, modifier = modifier
          ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Page")
          }
        }
      }
//      is BrowserWebPage -> {
//        { modifier ->
//          IconButton(
//            onClick = { scope.launch { page.webView.reload() } }, modifier = modifier
//          ) {
//            Icon(Icons.Rounded.Refresh, contentDescription = "Reload Web Page")
//          }
//        }
//      }
//
//      else -> if ( viewModel.isTabFixedSize && viewModel.pageSize > 1)  else null
//    }
    Row(
      modifier = Modifier.fillMaxSize(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = if (endIcon == null) Arrangement.Center else Arrangement.SpaceBetween,
    ) {
      val emptyTheme = page is BrowserHomePage
      /// 左边的图标，正方形大小，图标剧中
      Box(Modifier.size(dimenSearchHeight), contentAlignment = Alignment.Center) {
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
        Box(
          Modifier.aspectRatio(1f).clip(AppIconContainer.defaultShape),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painter,
            colorFilter = colorFilter,
            contentDescription = pageTitle,
            modifier = Modifier.size(24.dp),
          )
        }
      }
      Text(
        text = pageTitle.ifEmpty { humanPageUrl },
        modifier = if (endIcon == null) Modifier.padding(end = 32.dp) else Modifier.weight(1f),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleSmall
      )
      endIcon?.also {
        endIcon(Modifier.hoverCursor())
      }
    }

    /// 如果显示的标题不为空，那么就显示url到底部小字里
    if (pageTitle.isNotEmpty()) {
      Text(
        humanPageUrl,
        Modifier.fillMaxWidth().align(Alignment.BottomCenter).alpha(0.5f),
        textAlign = TextAlign.Center,
        fontSize = 5.sp,
        lineHeight = 8.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall,
      )
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

/**
 * 将合法的url，解析成需要显示的简要内容
 */
internal fun pageUrlTransformer(pageUrl: String, needHost: Boolean = true): String {
  if (
  // deeplink
    pageUrl.startsWith("dweb:")
    // 内部页面
    || pageUrl.startsWith("about:") || pageUrl.startsWith("chrome:")
    // android 特定的链接，理论上不应该给予支持
    || pageUrl.startsWith("file:///android_asset/")
  ) {
    return pageUrl
  }
  // 尝试当成网址来解析
  val url = pageUrl.toWebUrl() ?: return pageUrl
  return if (needHost && url.host.isNotEmpty()) {
    url.host.domainSimplify()
  } else pageUrl
}

/**
 * 尝试剔除 www.
 */
private fun String.domainSimplify() = if (startsWith("www.") && split('.').size == 3) {
  substring(4)
} else this