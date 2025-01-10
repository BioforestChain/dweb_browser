package org.dweb_browser.browser.web.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserWebPage
import org.dweb_browser.browser.web.ui.dimenPageHorizontal
import org.dweb_browser.dwebview.rememberHistoryCanGoBack
import org.dweb_browser.dwebview.rememberHistoryCanGoForward
import org.dweb_browser.sys.clipboard.ext.clipboardWriteText

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchWebPage(
  viewModel: BrowserViewModel,
  webPage: BrowserWebPage,
  searchTextState: TextFieldState,
  onDismissRequest: () -> Unit,
) {
  Column {
    val scope = rememberCoroutineScope()

    PanelTitle(
      webPage.title,
      titleIcon = {
        webPage.icon?.let { Image(it, "", modifier = Modifier.size(18.dp)) }
          ?: Icon(Icons.Rounded.Public, "")
      },
    )
    /// 关于URL的信息以及一些操作
    ListItem(modifier = Modifier.fillMaxWidth(), leadingContent = {
      Icon(
        imageVector = Icons.Rounded.Link,
        contentDescription = null,
      )
    }, headlineContent = {
      Text(text = webPage.url)
    }, supportingContent = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton({
          scope.launch {
            viewModel.browserNMM.clipboardWriteText(webPage.url)
          }
        }) {
          Icon(Icons.Rounded.ContentCopy, "copy url")
        }
        IconButton({
          searchTextState.setTextAndPlaceCursorAtEnd(webPage.url)
        }) {
          Icon(Icons.Default.Edit, "edit url")
        }
        IconButton({
          scope.launch {
            viewModel.shareWebSiteInfo(webPage)
          }
        }) {
          Icon(Icons.Default.Share, "shared link")
        }
        val added = webPage.isInBookmark
        IconButton({
          scope.launch {
            when {
              added -> viewModel.removeBookmarkUI(webPage.url)
              else -> viewModel.addBookmarkUI(webPage)
            }
          }
        }) {
          Icon(
            when {
              added -> Icons.Default.BookmarkRemove
              else -> Icons.Default.BookmarkAdd
            }, "bookmark"
          )
        }
      }
    })
    /// 关于网页的一些其它操作
    FlowRow(
      modifier = Modifier.padding(horizontal = dimenPageHorizontal).padding(top = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      @Composable
      fun ActionButton(
        onClick: suspend () -> Unit,
        text: String,
        icon: ImageVector,
        enabled: Boolean = true,
      ) {
        Button(
          {
            webPage.webView.lifecycleScope.launch {
              launch {
                delay(150)
                onDismissRequest()
              }
              onClick()
            }
          },
          contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
          enabled = enabled,
        ) {
          Icon(icon, "", modifier = Modifier.size(ButtonDefaults.IconSize))
          Spacer(modifier = Modifier.width(8.dp))
          Text(text)
        }
      }

      ActionButton(
        { webPage.webView.goBack() },
        text = BrowserI18nResource.browser_web_go_back(),
        icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
        enabled = webPage.webView.rememberHistoryCanGoBack()
      )
      ActionButton(
        { webPage.webView.historyGoForward() },
        text = BrowserI18nResource.browser_web_go_forward(),
        icon = Icons.AutoMirrored.Rounded.ArrowForwardIos,
        enabled = webPage.webView.rememberHistoryCanGoForward()
      )
      ActionButton(
        { webPage.webView.reload() },
        text = BrowserI18nResource.browser_web_refresh(),
        icon = Icons.Rounded.Refresh,
      )
    }

  }
}