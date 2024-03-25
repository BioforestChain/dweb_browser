package org.dweb_browser.browser.download

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.download.model.LocalDownloadModel
import org.dweb_browser.browser.download.ui.DownloadView
import org.dweb_browser.browser.download.ui.LocalDecompressModel
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.sys.window.core.WindowContentRenderScope

@Composable
fun DownloadController.Render(modifier: Modifier, windowRenderScope: WindowContentRenderScope) {
  LocalCompositionChain.current.Provider(
    LocalDownloadModel provides this.downloadModel,
    LocalDecompressModel provides this.decompressModel,
  ) {
    Box(modifier = with(windowRenderScope) {
      modifier
        .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
        .scale(scale)
    }) {
      DownloadView()
    }
  }
}