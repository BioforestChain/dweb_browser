package org.dweb_browser.browser.download.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate
import org.dweb_browser.browser.download.DownloadController
import org.dweb_browser.helper.compose.noLocalProvidedFor

val LocalDownloadModel = compositionLocalOf<DownloadModel> {
  noLocalProvidedFor("LocalDownloadModel")
}

enum class DownloadTab(val id: Int, val title: String, val vector: ImageVector) {
  Downloads(1, "Downloads", Icons.Default.FileDownload),
  Files(2, "Files", Icons.Default.FileOpen),
  ;
}

class DownloadModel(val downloadController: DownloadController) {
  val tabIndex = mutableIntStateOf(0)
  val tabItems = DownloadTab.values()
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