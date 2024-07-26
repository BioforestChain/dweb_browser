package org.dweb_browser.browser.download.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Downloading
import androidx.compose.material.icons.twotone.FileDownloadDone
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate
import org.dweb_browser.browser.download.DownloadController
import org.dweb_browser.browser.download.DownloadI18n
import org.dweb_browser.helper.compose.SimpleI18nResource

enum class DownloadListTabs(val title: SimpleI18nResource, val icon: ImageVector) {
  Downloading(DownloadI18n.downloading, Icons.TwoTone.Downloading),
  Completed(DownloadI18n.completed, Icons.TwoTone.FileDownloadDone),
  ;
}

class DownloadListModel(internal val downloadController: DownloadController) {
  var tabIndex by mutableIntStateOf(0)
  val tabItems = DownloadListTabs.entries.toTypedArray()
  fun startDownload(downloadTask: DownloadTask) = downloadController.launch {
    downloadController.startDownload(downloadTask)
  }

  fun pauseDownload(downloadTask: DownloadTask) = downloadController.launch {
    downloadController.pauseDownload(downloadTask)
  }

  fun close() = downloadController.launch { downloadController.close() }

  fun removeDownloadTask(downloadTask: DownloadTask) = downloadController.launch {
    downloadController.removeDownload(downloadTask.id)
  }
}

private fun main() {
  val time = LocalDate.toString()
  println(time)
}