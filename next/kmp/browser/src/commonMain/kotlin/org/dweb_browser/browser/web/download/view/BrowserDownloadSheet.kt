package org.dweb_browser.browser.web.download.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_state_init
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_state_install
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_state_open
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_state_pause
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_state_resume
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_tip_cancel
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_tip_continue
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_tip_exist
import org.dweb_browser.browser.web.download.BrowserDownloadI18nResource.sheet_download_tip_reload
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.download.BrowserDownloadModel
import org.dweb_browser.browser.web.data.BrowserDownloadType
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.helper.valueIn
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.imageFetchHook

/**
 * 下载弹出界面
 */
@Composable
fun BrowserDownloadModel.BrowserDownloadSheet(downloadItem: BrowserDownloadItem) {
  AnimatedContent(
    targetState = alreadyExist.value,
    transitionSpec = {
      if (targetState) {
        (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn()).togetherWith(
          slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
      } else {
        (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()).togetherWith(
          slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
      }
    }
  ) {
    when (it) {
      true -> {
        DownloadTip(downloadItem)
      }

      false -> {
        DownloadSheet(downloadItem)
      }
    }
  }
}

/**
 * 用于提示当前下载链接已存在，是否重新下载
 */
@Composable
private fun BrowserDownloadModel.DownloadTip(downloadItem: BrowserDownloadItem) {
  Card(elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
    Column(
      Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = sheet_download_tip_exist(),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Button(onClick = { close() }) {
          Text(text = sheet_download_tip_cancel())
        }

        Button(onClick = { clickRetryButton(downloadItem) }) {
          val text =
            if (downloadItem.state.valueIn(DownloadState.Downloading, DownloadState.Paused)) {
              sheet_download_tip_continue()
            } else sheet_download_tip_reload()
          Text(text = text)
        }
      }
    }
  }
}

/**
 * 下载的显示界面
 */
@Composable
private fun BrowserDownloadModel.DownloadSheet(downloadItem: BrowserDownloadItem) {
  val state = LocalWindowController.current.state
  val microModule by state.constants.microModule

  Card(elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
    Column(
      Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = sheet_download(),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AppIcon(
          icon = downloadItem.fileSuffix.icon,
          modifier = Modifier.size(56.dp),
          iconFetchHook = microModule?.imageFetchHook
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
            text = downloadItem.downloadArgs.contentLength?.toSpaceSize() ?: "",
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
          )
        }

        DownloadButton(downloadItem) { clickDownloadButton(downloadItem) }
      }
    }
  }
}

/**
 * 按钮显示内容
 * 根据showProgress来确认按钮是否要显示进度
 */
@Composable
fun DownloadButton(
  downloadItem: BrowserDownloadItem,
  showProgress: Boolean = true,
  onClick: () -> Unit
) {
  val showText = when (downloadItem.state.state) {
    DownloadState.Init, DownloadState.Canceled, DownloadState.Failed -> {
      sheet_download_state_init()
    }
    // 显示百分比
    DownloadState.Downloading -> {
      if (showProgress) {
        val progress = (downloadItem.state.current * 1000 / downloadItem.state.total) / 10.0f
        "$progress %"
      } else {
        sheet_download_state_pause()
      }
    }

    DownloadState.Paused -> sheet_download_state_resume()
    DownloadState.Completed -> {
      if (downloadItem.fileSuffix.type == BrowserDownloadType.Application)
        sheet_download_state_install()
      else
        sheet_download_state_open()
    }
  }

  val progress = if (showProgress &&
    downloadItem.state.state.valueIn(DownloadState.Downloading, DownloadState.Paused)
  ) {
    downloadItem.state.current * 1.0f / downloadItem.state.total
  } else {
    1.0f
  }
  Box(
    modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(32.dp)).width(90.dp)
      .background(
        brush = Brush.horizontalGradient(
          0.0f to MaterialTheme.colorScheme.primary,
          progress to MaterialTheme.colorScheme.primary,
          progress to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
          1.0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        )
      )
      .padding(vertical = 8.dp)
      .clickableWithNoEffect { onClick() },
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