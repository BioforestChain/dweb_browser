package org.dweb_browser.browser.download.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate

class DownloadModel {
}

fun getIconByMime(mime: String) : ImageVector {
  return when(mime) {
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