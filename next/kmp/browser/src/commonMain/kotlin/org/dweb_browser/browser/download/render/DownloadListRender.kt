package org.dweb_browser.browser.download.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadController
import org.dweb_browser.browser.download.model.DownloadListTabs
import org.dweb_browser.browser.download.model.DownloadState
import org.dweb_browser.browser.download.model.DownloadTask
import org.dweb_browser.helper.compose.NoDataRender

@Composable
fun DownloadController.DownloadList(
  modifier: Modifier,
  downloadClick: (DownloadTask) -> Unit,
) {
  Column(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
      downloadListModel.tabItems.forEachIndexed { index, downloadTab ->
        SegmentedButton(selected = index == downloadListModel.tabIndex,
          onClick = { downloadListModel.tabIndex = index },
          shape = SegmentedButtonDefaults.itemShape(
            index = index, count = downloadListModel.tabItems.size
          ),
          icon = {
            Icon(
              imageVector = downloadTab.icon, contentDescription = downloadTab.title()
            )
          },
          label = { Text(text = downloadTab.title()) })
      }
    }
    val downloadTab = downloadListModel.tabItems[downloadListModel.tabIndex]
    val downloadMap by downloadMapFlow.collectAsState()
    val list = remember(downloadMap, downloadTab) {
      when (downloadTab) {
        DownloadListTabs.Completed -> downloadMap.values.filter { it.status.state == DownloadState.Completed }
        DownloadListTabs.Downloading -> downloadMap.values.filter { it.status.state != DownloadState.Completed }
      }
    }.toList()

    LazyColumn(Modifier.fillMaxSize()) {
      if (list.isEmpty()) {
        item {
          NoDataRender(BrowserI18nResource.no_download_links())
        }
      }
      items(list, key = { it.id }) { downloadTask ->
        Column(Modifier.fillMaxSize()) {
          downloadListModel.DownloadItem(
            onClick = {
              downloadClick(downloadTask)
              decompressModel.hide()
            },
            onRemove = { downloadListModel.removeDownloadTask(downloadTask) },
            downloadTask = downloadTask,
          )
          HorizontalDivider()
        }
      }
    }
  }
}
