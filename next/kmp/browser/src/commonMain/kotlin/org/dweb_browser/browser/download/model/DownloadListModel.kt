package org.dweb_browser.browser.download.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.VideoFile
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
  Downloading(DownloadI18n.tab_downloading, Icons.TwoTone.Downloading),
  Completed(DownloadI18n.tab_completed, Icons.TwoTone.FileDownloadDone),
  ;
}

class DownloadListModel(private val downloadController: DownloadController) {
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

fun getIconByMime(mime: String): ImageVector {
  return when (mime) {
    "mp4", "avi", "rmvb", "" -> Icons.Default.VideoFile
    "mp3" -> Icons.Default.AudioFile
    "jpg", "png", "bmp", "svg" -> Icons.Default.Photo
    "apk" -> Icons.Default.Android
    else -> Icons.Default.FileDownload
  }
}

private fun main() {
  val time = LocalDate.toString()
  println(time)
}