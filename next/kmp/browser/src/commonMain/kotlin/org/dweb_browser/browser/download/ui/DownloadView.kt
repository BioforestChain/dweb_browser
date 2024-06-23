package org.dweb_browser.browser.download.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.CommonSimpleTopBar
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.model.DownloadTab
import org.dweb_browser.browser.download.model.LocalDownloadModel
import org.dweb_browser.helper.compose.LazySwipeColumn

@Composable
fun DownloadView(modifier: Modifier) {
  DownloadHistory(modifier)
  DecompressView(modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistory(modifier: Modifier) {
  val viewModel = LocalDownloadModel.current
  val scope = rememberCoroutineScope()
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    CommonSimpleTopBar(title = BrowserI18nResource.top_bar_title_download()) {
      scope.launch { viewModel.close() }
    }

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      viewModel.tabItems.forEachIndexed { index, downloadTab ->
        SegmentedButton(
          selected = index == viewModel.tabIndex.value,
          onClick = { viewModel.tabIndex.value = index },
          shape = RoundedCornerShape(16.dp),
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
    DownloadTabView()
  }
}

@Composable
fun DownloadTabView() {
  val viewModel = LocalDownloadModel.current
  val decompressModel = LocalDecompressModel.current
  val downloadTab = viewModel.tabItems[viewModel.tabIndex.value]
  val list = viewModel.downloadController.downloadList.filter {
    downloadTab == DownloadTab.Downloads || it.status.state == DownloadState.Completed
  }

  LazySwipeColumn(
    items = list,
    key = { item -> item.id },
    onRemove = { item -> viewModel.removeDownloadTask(item) },
    noDataValue = BrowserI18nResource.no_download_links()
  ) { downloadTask ->
    DownloadItem(downloadTask) { decompressModel.show(it) }
  }
}