package org.dweb_browser.browser.web.ui.search

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.BrowserViewModel

@Composable
internal fun SearchWeb3(
  viewModel: BrowserViewModel,
  searchText: String,
  onDismissRequest: () -> Unit,
) {
  LazyColumn {
    item {
      PanelTitle(
        BrowserI18nResource.browser_search_dwebapp(),
        titleIcon = { Icon(Icons.Default.AppShortcut, "") },
      )
    }
  }
}