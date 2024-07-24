package org.dweb_browser.browser.download.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadController
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.model.DownloadTab
import org.dweb_browser.helper.compose.ListDetailPaneScaffoldNavigator
import org.dweb_browser.helper.compose.NoDataRender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadController.DownloadHistory(
  navigator: ListDetailPaneScaffoldNavigator,
  modifier: Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
      downloadModel.tabItems.forEachIndexed { index, downloadTab ->
        SegmentedButton(
          selected = index == downloadModel.tabIndex,
          onClick = { downloadModel.tabIndex = index },
          shape = SegmentedButtonDefaults.itemShape(
            index = index,
            count = downloadModel.tabItems.size
          ),
          icon = {
            Icon(
              imageVector = downloadTab.vector,
              contentDescription = downloadTab.title()
            )
          },
          label = { Text(text = downloadTab.title()) }
        )
      }
    }
    val downloadTab = downloadModel.tabItems[downloadModel.tabIndex]
    val list = downloadModel.downloadController.downloadList.filter {
      downloadTab == DownloadTab.Downloads || it.status.state == DownloadState.Completed
    }
    LazyColumn {
      if (list.isEmpty()) {
        item {
          NoDataRender(BrowserI18nResource.no_download_links())
        }
      }
      items(list, key = { it.id }) { downloadTask ->
        DownloadItem(
          onClick = {
            decompressModel.show(downloadTask);
             navigator.navigateToDetail {
              decompressModel.hide()
            }
          },
          onRemove = { downloadModel.removeDownloadTask(downloadTask) },
          downloadTask = downloadTask,
        )
      }
    }
  }
}
