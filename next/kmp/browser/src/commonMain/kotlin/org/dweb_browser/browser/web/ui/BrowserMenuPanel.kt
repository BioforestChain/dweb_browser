package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivateConnectivity
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserWebPage
import org.dweb_browser.helper.PrivacyUrl
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.watchedState

@Composable
internal fun BrowserMenuPanel(modifier: Modifier = Modifier) {
  val uiScope = rememberCoroutineScope()
  val viewModel = LocalBrowserViewModel.current
  val scope = LocalBrowserViewModel.current.browserNMM.ioAsyncScope

  val hide = remember(viewModel) { { viewModel.showMore = false } }
  DropdownMenu(
    modifier = modifier,
    expanded = viewModel.showMore,
    onDismissRequest = { hide() }
  ) {
    val page = viewModel.focusedPage
    // 添加书签
    if (page is BrowserWebPage) {
      /// 添加到桌面
      SettingListItem(title = BrowserI18nResource.browser_menu_add_to_desktop(), // stringResource(id = R.string.browser_options_privacy),
        icon = Icons.AutoMirrored.Default.AddToHomeScreen, {
          hide()
          uiScope.launch { viewModel.addUrlToDesktopUI(page) }
        })
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

    SettingListItem(title = BrowserI18nResource.browser_menu_scanner(), // stringResource(id = R.string.browser_options_privacy),
      leadingIcon = {
        Icon(
          BrowserDrawResource.Scanner.painter(),
          contentDescription = "Open Camera To Scan",
          tint = LocalContentColor.current,
          modifier = Modifier.size(24.dp),
        )
      }, onClick = {
        viewModel.showQRCodePanel = true
        hide()
      })

    HorizontalDivider()
    // 搜索引擎
//        RowItemMenuView(
//          text = BrowserI18nResource.browser_options_search_engine(),
//          trailingIcon = Icons.Default.Settings
//        ) { openEngineManage() }

    // 下载管理界面
    Spacer(modifier = Modifier.height(12.dp))
    SettingListItem(
      title = BrowserI18nResource.Download.page_title(),
      icon = Icons.Default.FileDownload,
      onClick = {
        hide()
        uiScope.launch { viewModel.tryOpenUrlUI("about:downloads") }
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
  }

}

@Composable
private fun SettingListItem(
  title: String,
  leadingIcon: @Composable () -> Unit,
  onClick: () -> Unit = {},
  trailingIcon: (@Composable (() -> Unit))? = null
) {
  DropdownMenuItem(
    text = { Text(title) },
    onClick = onClick,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon
  )
}

@Composable
private fun SettingListItem(
  title: String,
  icon: ImageVector,
  onClick: () -> Unit = {},
  trailingContent: (@Composable (() -> Unit))? = null
) {
  SettingListItem(
    title = title,
    onClick = onClick,
    leadingIcon = { Icon(icon, contentDescription = title) },
    trailingIcon = trailingContent
  )
}

@Composable
private fun SettingListItem(
  title: String, icon: ImageVector, onClick: () -> Unit = {}, trailingIcon: ImageVector
) {
  SettingListItem(title, icon, onClick) {
    Icon(trailingIcon, contentDescription = title)
  }
}