package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.web.BrowserDownloadController
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.helper.valueIn

/**
 * 下载弹出界面
 */
@Composable
fun BrowserDownloadController.BrowserDownloadDialog() {
  // 显示一个提醒的 dialog， 另一个就是显示下载进度的
  curDownloadItem?.let { downloadItem ->
    if (alreadyExists) {
      DownloadTip(downloadItem)
    } else {
      DownloadTopToastBar(downloadItem)
    }
  }
}

/**
 * 用于提示当前下载链接已存在，是否重新下载
 */
@Composable
private fun BrowserDownloadController.DownloadTip(downloadItem: BrowserDownloadItem) {
  AlertDialog(onDismissRequest = { }, title = {
    Text(text = BrowserI18nResource.Download.dialog_retry_title())
  }, text = {
    val textArray = BrowserI18nResource.Download.dialog_retry_message().split("%s")
    Text(text = buildAnnotatedString {
      append(textArray.getOrNull(0))
      withStyle(
        style = SpanStyle(
          color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
        )
      ) {
        append(downloadItem.fileName)
      }
      append(textArray.getOrNull(1))
      append(
        downloadItem.downloadArgs.contentLength?.toSpaceSize()
          ?: BrowserI18nResource.Download.unknownSize()
      )
      append(textArray.getOrNull(2))
    })
  }, confirmButton = {
    TextButton(onClick = { clickRetryButton(downloadItem) }) {
      Text(BrowserI18nResource.Download.dialog_confirm())
    }
  }, dismissButton = {
    TextButton(onClick = { closeDownloadDialog() }) {
      Text(BrowserI18nResource.Download.dialog_cancel())
    }
  })
}

/**
 * 窗口顶部，下载提示框
 */
@Composable
private fun BrowserDownloadController.DownloadTopToastBar(downloadItem: BrowserDownloadItem) {
  Dialog(
    onDismissRequest = { closeDownloadDialog() },
    properties = DialogProperties(usePlatformDefaultWidth = false) // 关闭左右边的边距
  ) {
    // 全屏覆盖，方便设定对话框的绝对位置
    Column(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth().clickableWithNoEffect {
        closeDownloadDialog()
      })
      // 约束布局，使得对话框能在底部
      Column(
        modifier = Modifier.wrapContentHeight().fillMaxWidth()
          .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
          .background(MaterialTheme.colorScheme.background)
          .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = BrowserI18nResource.Download.dialog_download_title(),
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Image(
            painter = downloadItem.fileType.painter(),
            contentDescription = downloadItem.fileName,
            modifier = Modifier.size(56.dp),
          )

          Column(
            modifier = Modifier.weight(1f).height(56.dp),
            verticalArrangement = Arrangement.SpaceAround
          ) {
            Text(
              text = downloadItem.fileName,
              maxLines = 1,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = downloadItem.downloadArgs.contentLength?.toSpaceSize()
                ?: BrowserI18nResource.Download.unknownSize(),
              maxLines = 1,
              style = MaterialTheme.typography.bodySmall,
            )
          }

          DownloadButton(downloadItem) { clickDownloadButton(downloadItem) }
        }
      }
    }
  }
}

/**
 * 按钮显示内容
 * 根据showProgress来确认按钮是否要显示进度
 */
@Composable
private fun DownloadButton(
  downloadItem: BrowserDownloadItem, showProgress: Boolean = true, onClick: () -> Unit,
) {
  val showText = when (downloadItem.state.state) {
    DownloadState.Init, DownloadState.Canceled, DownloadState.Failed -> {
      BrowserI18nResource.Download.button_title_init()
    }
    // 显示百分比
    DownloadState.Downloading -> {
      if (showProgress) {
        downloadItem.state.percentProgress()
      } else {
        BrowserI18nResource.Download.button_title_pause()
      }
    }

    DownloadState.Paused -> BrowserI18nResource.Download.button_title_resume()
    DownloadState.Completed -> BrowserI18nResource.Download.button_title_open() // 完成后，直接显示为打开
  }

  val progress = if (showProgress && downloadItem.state.state.valueIn(
      DownloadState.Downloading, DownloadState.Paused
    )
  ) {
    downloadItem.state.progress()
  } else {
    1.0f
  }
  Box(
    modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(32.dp)).width(90.dp).background(
      brush = Brush.horizontalGradient(
        0.0f to MaterialTheme.colorScheme.primary,
        progress to MaterialTheme.colorScheme.primary,
        progress to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        1.0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
      )
    ).padding(vertical = 8.dp).clickableWithNoEffect { onClick() },
    contentAlignment = Alignment.Center
  ) {
    AutoResizeTextContainer(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = showText,
        softWrap = false,
        color = MaterialTheme.colorScheme.background,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier.align(Alignment.Center)
      )
    }
  }
}