package org.dweb_browser.browser.download.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadController
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.compose.compositionChainOf

val LocalDownloadModel = compositionChainOf<DownloadModel>("LocalDownloadModel")

enum class DownloadTab(val id: Int, val title: SimpleI18nResource, val vector: ImageVector) {
  Downloads(1, BrowserI18nResource.install_tab_download, Icons.Default.FileDownload),
  Files(2, BrowserI18nResource.install_tab_file, Icons.Default.FileOpen),
  ;
}

class DownloadModel(val downloadController: DownloadController) {
  var tabIndex by mutableIntStateOf(0)
  val tabItems = DownloadTab.entries.toTypedArray()
  suspend fun startDownload(downloadTask: DownloadTask) =
    downloadController.startDownload(downloadTask)

  suspend fun pauseDownload(downloadTask: DownloadTask) =
    downloadController.pauseDownload(downloadTask)

  suspend fun close() = downloadController.close()

  fun removeDownloadTask(downloadTask: DownloadTask) {
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