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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.browser.jmm.ui.JmmIntent
import org.dweb_browser.browser.jmm.ui.LocalJmmViewHelper
import kotlinx.coroutines.launch
import org.dweb_browser.core.sys.download.JmmDownloadStatus
import org.dweb_browser.helper.toSpaceSize

@Composable
internal fun BoxScope.BottomDownloadButton() {
  val background = MaterialTheme.colorScheme.surface
  val scope = rememberCoroutineScope()
  val viewModel = LocalJmmViewHelper.current

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .align(Alignment.BottomCenter)
      .background(
        brush = Brush.verticalGradient(listOf(background.copy(0f), background))
      )
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    val downloadStatus = viewModel.uiState.downloadStatus.value
    val downloadSize = viewModel.uiState.downloadSize.value
    val totalSize = viewModel.uiState.jmmAppInstallManifest.bundle_size
    var showLinearProgress = false
    val canSupportTarget = remember {
      viewModel.uiState.jmmAppInstallManifest.canSupportTarget(JsMicroModule.VERSION)
    }
    val text = if (canSupportTarget) when (downloadStatus) {
      JmmDownloadStatus.Init, JmmDownloadStatus.Canceld -> {
        "下载 (${totalSize.toSpaceSize()})"
      }

      JmmDownloadStatus.NewVersion -> {
        "更新 (${totalSize.toSpaceSize()})"
      }

      JmmDownloadStatus.Downloading -> {
        showLinearProgress = true
        "下载中 ${downloadSize.toSpaceSize()} / ${totalSize.toSpaceSize()}"
      }

      JmmDownloadStatus.Paused -> {
        showLinearProgress = true
        "暂停 ${downloadSize.toSpaceSize()} / ${totalSize.toSpaceSize()}"
      }

      JmmDownloadStatus.Completed -> "安装中..."
      JmmDownloadStatus.INSTALLED -> "打开"
      JmmDownloadStatus.Failed -> "重新下载"
    } else "该应用与您的设备不兼容"

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
        scope.launch {
          viewModel.handlerIntent(JmmIntent.ButtonFunction)
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