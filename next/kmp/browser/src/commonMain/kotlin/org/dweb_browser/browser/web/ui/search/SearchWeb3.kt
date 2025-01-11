package org.dweb_browser.browser.web.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ktor.http.headers
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.ui.IconRender
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod

@Composable
internal fun SearchWeb3(
  viewModel: BrowserViewModel,
  web3Searcher: Web3Searcher?,
  onDismissRequest: () -> Unit,
  onSuggestionActions: OnSuggestionActions,
) {
  LaunchedEffect(web3Searcher) {
    web3Searcher?.doSearchDwebapps?.invoke()
  }
  LazyColumn {
    /// 日志信息
    item {
      var isOpen by remember { mutableStateOf(false) }

      Column(Modifier.fillMaxWidth()) {
        val logs = web3Searcher?.logList ?: emptyList()
        ListItem(
          leadingContent = {
            Icon(Icons.AutoMirrored.Rounded.ReceiptLong, "")
          },
          headlineContent = {
            Text(BrowserI18nResource.browser_web3_search_logs())
          },
          supportingContent = {
            Text(
              when {
                isOpen -> web3Searcher?.previewLog?.value ?: ""
                else -> logs.lastOrNull() ?: ""
              },
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          trailingContent = {
            IconButton({ isOpen = !isOpen }) {
              Icon(
                when {
                  isOpen -> Icons.Rounded.ExpandLess
                  else -> Icons.Rounded.ExpandMore
                }, ""
              )
            }
          },
        )
        if (isOpen) {
          LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 360.dp)
              .background(MaterialTheme.colorScheme.surfaceContainer)
          ) {
            items(logs.size) {
              val log = logs[logs.size - it - 1]
              val first = it == 0
              val last = it == logs.size - 1
              Text(
                log,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp).padding(
                  top = if (first) 8.dp else 0.dp,
                  bottom = if (last) 8.dp else 0.dp,
                )
              )
              if (!last) {
                HorizontalDivider()
              }
            }
          }
        }
        HorizontalDivider()
      }
    }
    /// 标题和概览
    val dwebappsEntries = (web3Searcher?.dwebappMap ?: emptyMap()).toList()
    item {
      PanelTitle(
        titleText = BrowserI18nResource.browser_search_dwebapp(),
        titleIcon = { Icon(Icons.Default.AppShortcut, "") },
        enabled = web3Searcher != null,
        trailingContent = {
          if (dwebappsEntries.isNotEmpty()) {
            Text(
              BrowserI18nResource.browser_web3_found_dwebapps(dwebappsEntries.size.toString()),
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.alpha(0.6f)
            )
          }
        },
      )
    }
    /// 找到的应用列表
    items(dwebappsEntries.size) {
      val (_, apps) = dwebappsEntries[it]
      val info = apps.first()
      val app = info.app
      val first = it == 0
      val last = it == dwebappsEntries.size - 1
      if (!first) {
        HorizontalDivider()
      }
      ListItem(
        modifier = Modifier.clickable {
          viewModel.browserNMM.scopeLaunch(cancelable = true) {
            viewModel.browserNMM.nativeFetch(
              PureClientRequest(
                "dweb://install?url=${info.manifestUrl.encodeURIComponent()}",
                PureMethod.GET,
                headers = PureHeaders(headers = headers {
                  "Referrer" to info.originUrl
                })
              )
            )
          }
        },
        leadingContent = {
          app.IconRender(64.dp)
        },
        headlineContent = {
          Column {
            // overline content
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmall) {
                Text(app.id)
                Text("v" + app.version)
              }
            }
            // headline content
            Text(app.name)
          }
        },
        supportingContent = {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip({}, label = { Text(app.bundle_size.toSpaceSize()) })
            /// 一些认证信息的徽章
            // FilterChip()
            // SuggestionChip()
          }
        },
        trailingContent = {
          Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, "Go To Detail")
        },
      )
      if (!last) {
        HorizontalDivider()
      }
    }
  }
}
