package info.bagen.dwebbrowser.microService.browser.jmm.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.jmm.ui.JmmUIState
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect
import org.dweb_browser.browserUI.download.DownLoadStatus

@Composable
internal fun BoxScope.BottomDownloadButton(
  jmmUIState: JmmUIState, onClick: () -> Unit
) {
  val background = MaterialTheme.colorScheme.surface
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .align(Alignment.BottomCenter)
      .background(
        brush = Brush.verticalGradient(listOf(background.copy(0f), background))
      )
  ) {
    val downloadStatus = jmmUIState.downloadStatus.value
    val downloadSize = jmmUIState.downloadSize.value
    val totalSize = jmmUIState.jmmAppInstallManifest.bundle_size
    var showLinearProgress = false
    val text = when (downloadStatus) {
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
    }

    val modifier = Modifier
      .padding(horizontal = 64.dp, vertical = 16.dp)
      .shadow(elevation = 2.dp, shape = RoundedCornerShape(ShapeCorner))
      .fillMaxWidth()
      .height(50.dp)
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

    Box(
      modifier = m2.clickableWithNoEffect { onClick() }, contentAlignment = Alignment.Center
    ) {
      Text(text = text, color = MaterialTheme.colorScheme.onPrimary)
    }
  }
}