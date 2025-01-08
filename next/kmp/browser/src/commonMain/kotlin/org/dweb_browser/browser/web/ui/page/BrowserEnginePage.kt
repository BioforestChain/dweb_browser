package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.search.SearchEngine
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserEnginePage
import org.dweb_browser.browser.web.ui.common.BrowserTopBar
import org.dweb_browser.helper.compose.NoDataRender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserEnginePageRender(enginePage: BrowserEnginePage, modifier: Modifier) {
  Column(modifier = modifier) {
    BrowserTopBar(
      title = BrowserI18nResource.Engine.page_title(),
      enableNavigation = false
    )
    BrowserSearchEngineListPage(enginePage = enginePage)
  }
}

@Composable
private fun BrowserSearchEngineListPage(
  modifier: Modifier = Modifier,
  enginePage: BrowserEnginePage,
) {
  val viewModel = LocalBrowserViewModel.current
  val list = viewModel.filterAllEngines
  if (list.isEmpty()) {
    NoDataRender(BrowserI18nResource.browser_empty_list(), modifier = modifier)
    return
  }

  LazyColumn(modifier = Modifier.fillMaxSize()) {
    items(list) { engineItem ->
      ListItem(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        headlineContent = {
          Text(text = engineItem.displayName)
        },
        supportingContent = {
          Text(text = engineItem.homeLink)
        },
        leadingContent = {
          Image(painter = engineItem.painter(), contentDescription = engineItem.name)
        },
        trailingContent = {
          SearchEngineToggleButton(engineItem, viewModel)
        }
      )
    }
  }
}

@Composable
fun SearchEngineToggleButton(
  engineItem: SearchEngine,
  viewModel: BrowserViewModel,
  modifier: Modifier = Modifier,
) {
  var isShowMenu by remember { mutableStateOf(false) }
  IconButton({ isShowMenu = true }, modifier = modifier) {
    Icon(
      imageVector = if (engineItem.enable) Icons.Outlined.Search else Icons.Outlined.SearchOff,
      contentDescription = if (engineItem.enable) "Search" else "SearchOff",
      tint =
      if (engineItem.enable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
    )
  }
  DropdownMenu(expanded = isShowMenu, onDismissRequest = { isShowMenu = false }) {
    DropdownMenuItem(
      onClick = {
        isShowMenu = false
        if (!engineItem.enable) viewModel.enableSearchEngine(engineItem)
      },
      text = { Text(text = BrowserI18nResource.Engine.status_enable()) },
      leadingIcon = {
        Icon(
          imageVector = Icons.Outlined.Search,
          contentDescription = "Enable",
          tint = MaterialTheme.colorScheme.primary
        )
      }
    )
    DropdownMenuItem(
      onClick = {
        isShowMenu = false
        if (engineItem.enable) viewModel.disableSearchEngine(engineItem)
      },
      text = { Text(text = BrowserI18nResource.Engine.status_disable()) },
      leadingIcon = {
        Icon(
          imageVector = Icons.Outlined.SearchOff,
          contentDescription = "Disable",
          tint = MaterialTheme.colorScheme.secondary
        )
      }
    )
  }
}
