package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.data.formatToStickyName
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserHistoryPage
import org.dweb_browser.browser.web.ui.common.BrowserTopBar
import org.dweb_browser.helper.compose.NoDataRender
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.datetimeNowToEpochDay
import org.dweb_browser.sys.window.render.LocalWindowController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserHistoryPageRender(historyPage: BrowserHistoryPage, modifier: Modifier) {
  LocalWindowController.current.navigation.GoBackHandler(historyPage.isInEditMode) {
    historyPage.isInEditMode = false
  }
  val uiScope = rememberCoroutineScope()
  val viewModel = LocalBrowserViewModel.current
  Column(modifier = modifier) {
    BrowserTopBar(title = BrowserI18nResource.History.page_title(),
      enableNavigation = historyPage.isInEditMode,
      onNavigationBack = { historyPage.isInEditMode = false },
      actions = {
        if (historyPage.isInEditMode) {
          IconButton(enabled = historyPage.selectedHistories.isNotEmpty(), onClick = {
            uiScope.launch {
              viewModel.removeHistoryLink(historyPage.selectedHistories.toList())
              historyPage.isInEditMode = false
            }
          }) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete Selects")
          }
        } else {
          IconButton(onClick = {
            historyPage.isInEditMode = true
            historyPage.selectedHistories.clear()
          }) {
            Icon(
              imageVector = Icons.Filled.Edit, contentDescription = "Go to Edit"
            )
          }
        }
      })
    BrowserHistoryListPage(historyPage = historyPage)
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserHistoryListPage(modifier: Modifier = Modifier, historyPage: BrowserHistoryPage) {
  val uiScope = rememberCoroutineScope()
  val viewModel = LocalBrowserViewModel.current
  val historyMap = viewModel.getHistoryLinks()
  if (historyMap.isEmpty()) {
    NoDataRender(BrowserI18nResource.browser_empty_list(), modifier = modifier)
    return
  }

  val currentDay = datetimeNowToEpochDay()

  LazyColumn(modifier.background(MaterialTheme.colorScheme.background).padding(vertical = 16.dp)) {
    for (day in currentDay downTo currentDay - 6) {
      val historyList = historyMap[day.toString()] ?: continue
      stickyHeader(key = day) {
        Text(
          text = day.formatToStickyName(),
          modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp),
          fontWeight = FontWeight(500),
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.outline
        )
      }

      items(historyList) { historyItem ->
        val openInNewPage = remember(viewModel, historyItem) {
          {
            uiScope.launch { viewModel.tryOpenUrlUI(historyItem.url) }
            Unit
          }
        }
        ListItem(headlineContent = {
          Text(text = historyItem.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
        }, modifier = Modifier.clickableWithNoEffect {
          if (!historyPage.isInEditMode) {
            openInNewPage()
          }
        }, supportingContent = {
          Text(text = historyItem.url, overflow = TextOverflow.Ellipsis, maxLines = 2)
        }, trailingContent = {
          if (historyPage.isInEditMode) {
            Checkbox(checked = historyPage.selectedHistories.contains(historyItem), {
              when (it) {
                true -> historyPage.selectedHistories.add(historyItem)
                else -> historyPage.selectedHistories.remove(historyItem)
              }
            })
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "Open In New Page",
              tint = MaterialTheme.colorScheme.outlineVariant
            )
          }
        })
      }
    }
  }
}
