package org.dweb_browser.browser.download.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.model.DownloadTab
import org.dweb_browser.browser.download.model.LocalDownloadModel

@Composable
fun DownloadView() {
  DownloadTab()
}

@Composable
fun DownloadTab() {
  val viewModel = LocalDownloadModel.current
  Column {
    TabRow(selectedTabIndex = viewModel.tabIndex.value) {
      viewModel.tabItems.forEachIndexed { index, downloadTab ->
        Tab(
          selected = index == viewModel.tabIndex.value,
          onClick = { viewModel.tabIndex.value = index },
          text = { Text(text = downloadTab.title) },
          icon = { Icon(imageVector = downloadTab.vector, contentDescription = downloadTab.title) }
        )
      }
    }
    DownloadTabView()
  }
}

@Composable
fun DownloadTabView() {
  val viewModel = LocalDownloadModel.current
  val downloadTab = viewModel.tabItems[viewModel.tabIndex.value]
  val list = when (downloadTab) {
    DownloadTab.Downloads -> viewModel.downloadController.downloadManagers
    DownloadTab.Files -> viewModel.downloadController.downloadCompletes
  }
  if (list.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(text = BrowserI18nResource.no_download_links())
    }
    return
  }

  LazyColumn {
    list.forEach { downloadItem ->
      item(downloadItem.key) {
        DownloadItem(downloadItem.value, downloadTab)
        Spacer(
          Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.outlineVariant)
        )
      }
    }
  }
}