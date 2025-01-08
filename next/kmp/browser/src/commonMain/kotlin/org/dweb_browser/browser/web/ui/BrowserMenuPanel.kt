package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivateConnectivity
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserWebPage
import org.dweb_browser.dwebview.rememberHistoryCanGoBack
import org.dweb_browser.dwebview.rememberHistoryCanGoForward
import org.dweb_browser.helper.PrivacyUrl
import org.dweb_browser.helper.compose.ComposeWindowFocusOwnerEffect
import org.dweb_browser.helper.compose.ScalePopupContent
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

@Composable
internal fun BrowserMenuPanel(scale: Float, modifier: Modifier = Modifier) {
  val uiScope = rememberCoroutineScope()
  val viewModel = LocalBrowserViewModel.current
  val scope = LocalBrowserViewModel.current.browserNMM.getRuntimeScope()

  val hide = { viewModel.showMore = false }
  ComposeWindowFocusOwnerEffect(viewModel.showMore, hide)
  DropdownMenu(
    modifier = modifier,
    expanded = viewModel.showMore,
    onDismissRequest = hide,
    shape = SquircleShape((16 * scale).dp, CornerSmoothing.Small),
  ) {
    val page = viewModel.focusedPage
    /// 添加书签
    if (page is BrowserWebPage) {
      val added = page.isInBookmark
      if (added) {
        SettingListItem(title = BrowserI18nResource.browser_remove_bookmark(),
          icon = Icons.Default.BookmarkRemove,
          onClick = {
            hide()
            uiScope.launch { viewModel.removeBookmarkUI(page.url) }
          })
      } else {
        SettingListItem(title = BrowserI18nResource.browser_add_bookmark(),
          icon = Icons.Default.BookmarkAdd,
          onClick = {
            hide()
            uiScope.launch { viewModel.addBookmarkUI(page) }
          })
      }
    }
    SettingListItem(
      title = BrowserI18nResource.Bookmark.page_title(),
      icon = Icons.Default.Bookmark,
      onClick = {
        hide()
        uiScope.launch { viewModel.tryOpenUrlUI("about:bookmarks") }
      },
      trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos
    )
    // 分享
    if (page is BrowserWebPage) {
      HorizontalDivider()
      SettingListItem(
        title = BrowserI18nResource.browser_options_share(),
        icon = Icons.Default.Share,
        onClick = {
          hide()
          scope.launch { viewModel.shareWebSiteInfo(page) }
        })
      /// 添加到桌面
      SettingListItem(title = BrowserI18nResource.browser_menu_add_to_desktop(), // stringResource(id = R.string.browser_options_privacy),
        icon = Icons.AutoMirrored.Default.AddToHomeScreen, {
          hide()
          uiScope.launch { viewModel.addUrlToDesktopUI(page) }
        })
    }

    HorizontalDivider()
    // 无痕浏览
    SettingListItem(
      title = BrowserI18nResource.browser_options_noTrace(),
      icon = Icons.Default.PrivateConnectivity,
    ) {
      Switch(checked = viewModel.isIncognitoOn,
        onCheckedChange = { uiScope.launch { viewModel.updateIncognitoModeUI(it) } },
        thumbContent = if (viewModel.isIncognitoOn) {
          {
            Icon(
              imageVector = Icons.Filled.PrivateConnectivity,
              contentDescription = null,
              modifier = Modifier.size(SwitchDefaults.IconSize),
            )
          }
        } else null)
    }

    // 隐私政策
    SettingListItem(
      title = BrowserI18nResource.browser_options_privacy(), // stringResource(id = R.string.browser_options_privacy),
      icon = Icons.Default.Policy, onClick = {
        hide()
        uiScope.launch { viewModel.tryOpenUrlUI(PrivacyUrl) }
      }, trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos
    )

    // 扫码功能
    SettingListItem(title = BrowserI18nResource.browser_menu_scanner(), // stringResource(id = R.string.browser_options_privacy),
      leadingIcon = {
        Icon(
          BrowserDrawResource.Scanner.painter(),
          contentDescription = "Open Camera To Scan",
          tint = LocalContentColor.current,
          modifier = Modifier.size(24.dp),
        )
      }, onClick = {
        viewModel.showQRCodePanelUI()
        hide()
      })

    HorizontalDivider()
    // 搜索引擎
    SettingListItem(
      title = BrowserI18nResource.Engine.page_title(),
      icon = Icons.Default.PersonSearch,
      onClick = {
        uiScope.launch { viewModel.tryOpenUrlUI("about:engines") }
        hide()
      },
      trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos
    )
    // 下载管理界面
    SettingListItem(
      title = BrowserI18nResource.Download.page_title(),
      icon = Icons.Default.FileDownload,
      onClick = {
        uiScope.launch { viewModel.tryOpenUrlUI("about:downloads") }
        hide()
      },
      trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos
    )
    // 历史列表
    SettingListItem(
      title = BrowserI18nResource.History.page_title(),
      icon = Icons.Default.History,
      onClick = {
        hide()
        uiScope.launch { viewModel.tryOpenUrlUI("about:history") }
      },
      trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos
    )

    if (page is BrowserWebPage) {
      HorizontalDivider()
      ScalePopupContent {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
          IconButton(
            { page.webView.lifecycleScope.launch { page.webView.goBack() } },
            enabled = page.webView.rememberHistoryCanGoBack()
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, "go back")
          }
          VerticalDivider()
          IconButton(
            { page.webView.lifecycleScope.launch { page.webView.historyGoForward() } },
            enabled = page.webView.rememberHistoryCanGoForward()
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, "go forward")
          }
          VerticalDivider()
          IconButton(
            { page.webView.lifecycleScope.launch { page.webView.reload() } },
          ) {
            Icon(Icons.Rounded.Refresh, "reload web page")
          }
        }
      }
    }
  }
}


@Composable
private fun SettingListItem(
  title: String,
  leadingIcon: @Composable () -> Unit,
  onClick: () -> Unit = {},
  trailingIcon: (@Composable (() -> Unit))? = null,
) {
  ScalePopupContent {
    DropdownMenuItem(
      text = { Text(title) },
      onClick = onClick,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon
    )
  }
}

@Composable
private fun SettingListItem(
  title: String,
  icon: ImageVector,
  onClick: () -> Unit = {},
  trailingContent: (@Composable (() -> Unit))? = null,
) {
  SettingListItem(
    title = title,
    onClick = onClick,
    leadingIcon = { Icon(icon, contentDescription = title) },
    trailingIcon = trailingContent,
  )
}

@Composable
private fun SettingListItem(
  title: String,
  icon: ImageVector,
  onClick: () -> Unit = {},
  trailingIcon: ImageVector,
) {
  SettingListItem(title, icon, onClick) {
    Icon(trailingIcon, contentDescription = title)
  }
}