package info.bagen.dwebbrowser.microService.browser.jmm.render.appinstall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.jmm.JmmUIState
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule
import org.dweb_browser.browserUI.download.DownLoadStatus

@Composable
internal fun BottomDownloadButton(
  modifier: Modifier = Modifier,
  jmmUIState: JmmUIState, onClick: () -> Unit
) {
  val background = MaterialTheme.colorScheme.surface

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        brush = Brush.verticalGradient(listOf(background.copy(0f), background))
      )
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    val downloadStatus = jmmUIState.downloadStatus.value
    val downloadSize = jmmUIState.downloadSize.value
    val totalSize = jmmUIState.jmmAppInstallManifest.bundle_size
    var showLinearProgress = false
    val canSupportTarget = remember {
      jmmUIState.jmmAppInstallManifest.canSupportTarget(JsMicroModule.VERSION)
    }
    val text = if (canSupportTarget) when (downloadStatus) {
      DownLoadStatus.IDLE, DownLoadStatus.CANCEL -> {
        "下载 (${totalSize.toSpaceSize()})"
      }

      DownLoadStatus.NewVersion -> {
        "更新 (${totalSize.toSpaceSize()})"
      }

      DownLoadStatus.DownLoading -> {
        showLinearProgress = true
        "下载中".displayDownLoad(totalSize, downloadSize)
      }

      DownLoadStatus.PAUSE -> {
        showLinearProgress = true
        "暂停".displayDownLoad(totalSize, downloadSize)
      }

      DownLoadStatus.DownLoadComplete -> "安装中..."
      DownLoadStatus.INSTALLED -> "打开"
      DownLoadStatus.FAIL -> "重新下载"
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
      onClick = onClick,
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