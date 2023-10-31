package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.browser.jmm.ui.JmmStatus
import org.dweb_browser.browser.jmm.ui.LocalJmmViewHelper

@Composable
internal fun BoxScope.BottomDownloadButton() {
  val background = MaterialTheme.colorScheme.surface
  val viewModel = LocalJmmViewHelper.current

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .align(Alignment.BottomCenter)
      .background(
        brush = Brush.verticalGradient(listOf(background.copy(0f), background))
      )
      .padding(16.dp), contentAlignment = Alignment.Center
  ) {
    var downloadStatus by viewModel.uiState.downloadStatus
    val downloadSize = viewModel.uiState.downloadSize.value
    val totalSize = viewModel.uiState.jmmAppInstallManifest.bundle_size
    var showLinearProgress = false
    val canSupportTarget = remember {
      viewModel.uiState.jmmAppInstallManifest.canSupportTarget(JsMicroModule.VERSION)
    }
    val installByteLength = BrowserI18nResource.Companion.InstallByteLength(downloadSize, totalSize)
    val text = if (canSupportTarget) when (downloadStatus) {
      JmmStatus.Init, JmmStatus.Canceled -> {
        BrowserI18nResource.install_button_download(installByteLength)
      }

      JmmStatus.NewVersion -> {
        BrowserI18nResource.install_button_update(installByteLength)
      }

      JmmStatus.Downloading -> {
        showLinearProgress = true
        BrowserI18nResource.install_button_downloading(installByteLength)
      }

      JmmStatus.Paused -> {
        showLinearProgress = true
        BrowserI18nResource.install_button_paused(installByteLength)
      }

      JmmStatus.Completed -> BrowserI18nResource.install_button_installing()
      JmmStatus.INSTALLED -> BrowserI18nResource.install_button_open()
      JmmStatus.Failed -> BrowserI18nResource.install_button_retry()
    } else BrowserI18nResource.install_button_incompatible()

    val modifier = Modifier
      .requiredSize(height = 50.dp, width = 300.dp)
      .fillMaxWidth()
      .clip(ButtonDefaults.elevatedShape)
    val m2 = if (showLinearProgress) {
      val percent = if (totalSize == 0L) {
        0f
      } else {
        downloadSize * 1.0f / totalSize
      }
      modifier.background(
        Brush.horizontalGradient(
          0.0f to MaterialTheme.colorScheme.primary,
          maxOf(percent - 0.02f, 0.0f) to MaterialTheme.colorScheme.primary,
          minOf(percent + 0.02f, 1.0f) to MaterialTheme.colorScheme.outlineVariant,
          1.0f to MaterialTheme.colorScheme.outlineVariant
        )
      )
    } else {
      modifier.background(MaterialTheme.colorScheme.primary)
    }

    ElevatedButton(
      onClick = {
        when (downloadStatus) {
          JmmStatus.Init, JmmStatus.Failed, JmmStatus.Canceled, JmmStatus.NewVersion -> {
            downloadStatus = JmmStatus.Downloading
            viewModel.startDownload()
          }

          JmmStatus.Downloading -> {
            downloadStatus = JmmStatus.Paused
            viewModel.pause()
          }

          JmmStatus.Paused -> {
            downloadStatus = JmmStatus.Downloading
            viewModel.start()
          }

          JmmStatus.Completed -> {}
          JmmStatus.INSTALLED -> {
            viewModel.open()
          }
        }
      },
      modifier = m2,
      colors = ButtonDefaults.elevatedButtonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
        disabledContentColor = MaterialTheme.colorScheme.onErrorContainer,
      ),
      enabled = canSupportTarget,
    ) {
      Text(text = text)
    }
  }
}