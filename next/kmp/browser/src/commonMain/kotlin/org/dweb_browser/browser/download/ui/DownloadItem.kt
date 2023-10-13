package org.dweb_browser.browser.download.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.model.getIconByMime
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.toSpaceSize

@Composable
fun DownloadItem(downloadTask: DownloadTask) {
  ListItem(
    headlineContent = { // 主标题
      Text(text = downloadTask.url)
    },
    supportingContent = { // 副标题
      Column {
        val status = downloadTask.status
        LinearProgressIndicator(
          progress = status.current / status.total.toFloat(),
          color = when (status.state) {
            DownloadState.Downloading -> {
              MaterialTheme.colorScheme.primary
            }

            else -> {
              MaterialTheme.colorScheme.background
            }
          },
        )
        Row {
          Text("下载中 ${downloadTask.status.current.toSpaceSize()} / ${downloadTask.status.total.toSpaceSize()}")
          Text(downloadTask.mime)
        }
      }
    },
    leadingContent = { // 左边的图标
      Image(imageVector = getIconByMime(downloadTask.mime), contentDescription = "Downloading")
    },
    trailingContent = { // 右边的图标
      Row {
        Image(
          imageVector = if (downloadTask.status.state == DownloadState.Paused) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = "State",
          modifier = Modifier.clickableWithNoEffect {  }
        )
        Image(
          imageVector = Icons.Default.Close,
          contentDescription = "Cancel",
          modifier = Modifier.clickableWithNoEffect {  }
        )
      }
    }
  )
}