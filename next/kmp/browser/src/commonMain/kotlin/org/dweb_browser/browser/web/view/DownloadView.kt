package org.dweb_browser.browser.web.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.data.BrowserDownloadType
import org.dweb_browser.browser.web.model.BrowserDownloadModel
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.helper.valueIn
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.imageFetchHook

@Composable
fun BrowserDownloadModel.BrowserDownloadSheet(downloadItem: BrowserDownloadItem) {
  AnimatedContent(targetState = alreadyExist.value) {
    when (it) {
      true -> {
        BrowserDownloadTip(downloadItem)
      }

      false -> {
        BrowserDownloadView(downloadItem)
      }
    }
  }
}

@Composable
private fun BrowserDownloadModel.BrowserDownloadTip(downloadItem: BrowserDownloadItem) {
  Card(elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
    Column(
      Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = BrowserI18nResource.sheet_download_tip_exist(),
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
          Text(text = BrowserI18nResource.sheet_download_tip_cancel())
        }

        Button(onClick = { alreadyExist.value = false }) {
          val text =
            if (downloadItem.state.valueIn(DownloadState.Downloading, DownloadState.Paused)) {
              BrowserI18nResource.sheet_download_tip_continue()
            } else BrowserI18nResource.sheet_download_tip_reload()
          Text(text = text)
        }
      }
    }
  }
}

@Composable
fun BrowserDownloadModel.BrowserDownloadView(downloadItem: BrowserDownloadItem) {
  val state = LocalWindowController.current.state
  val microModule by state.constants.microModule

  Card(elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
    Column(
      Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = BrowserI18nResource.sheet_download(),
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
            text = downloadItem.downloadArgs.contentLength.toSpaceSize(),
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
          )
        }

        val progress =
          if (downloadItem.state.state.valueIn(DownloadState.Downloading, DownloadState.Paused)) {
            downloadItem.state.current * 1.0f / downloadItem.state.total
          } else {
            1.0f
          }
        Box(
          modifier = Modifier.clip(RoundedCornerShape(32.dp)).widthIn(min = 90.dp)
            .background(
              brush = Brush.horizontalGradient(
                0.0f to MaterialTheme.colorScheme.primary,
                progress to MaterialTheme.colorScheme.primary,
                progress to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                1.0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
              )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickableWithNoEffect { clickDownloadButton(downloadItem) },
          contentAlignment = Alignment.Center
        ) {
          Text(text = ButtonText(downloadItem), color = MaterialTheme.colorScheme.background)
        }
      }
    }
  }
}

@Composable
private fun ButtonText(downloadItem: BrowserDownloadItem): String {
  return when (downloadItem.state.state) {
    DownloadState.Init, DownloadState.Canceled, DownloadState.Failed -> BrowserI18nResource.sheet_download_state_init()
    DownloadState.Downloading -> {
      val progress = (downloadItem.state.current * 1000 / downloadItem.state.total) / 10.0f
      "$progress %"
    } // 显示百分比
    DownloadState.Paused -> BrowserI18nResource.sheet_download_state_pause()
    DownloadState.Completed -> {
      if (downloadItem.fileSuffix.type == BrowserDownloadType.Application)
        BrowserI18nResource.sheet_download_state_install()
      else
        BrowserI18nResource.sheet_download_state_open()
    }
  }
}