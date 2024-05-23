package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
  enginePage: BrowserEnginePage
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
          var isShowMenu by remember { mutableStateOf(false) }
          Icon(
            imageVector = if (engineItem.enable) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
            contentDescription = if (engineItem.enable) "Visibility" else "VisibilityOff",
            tint = if (engineItem.enable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.clickable { isShowMenu = true }
          )
          DropdownMenu(expanded = isShowMenu, onDismissRequest = { isShowMenu = false }) {
            DropdownMenuItem(
              onClick = {
                isShowMenu = false
                if (!engineItem.enable) viewModel.enableSearchEngine(engineItem)
              },
              text = { Text(text = BrowserI18nResource.Engine.status_enable()) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Outlined.Visibility,
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
                  imageVector = Icons.Outlined.VisibilityOff,
                  contentDescription = "Disable",
                  tint = MaterialTheme.colorScheme.secondary
                )
              }
            )
          }
        }
      )
    }
  }
}
