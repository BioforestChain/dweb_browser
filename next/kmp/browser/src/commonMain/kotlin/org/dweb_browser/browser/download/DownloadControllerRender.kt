package org.dweb_browser.browser.download

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.download.model.DownloadModel
import org.dweb_browser.browser.download.model.LocalDownloadModel
import org.dweb_browser.browser.download.ui.DownloadView
import org.dweb_browser.sys.window.core.WindowRenderScope

@Composable
fun DownloadController.Render(modifier: Modifier, windowRenderScope: WindowRenderScope) {
  CompositionLocalProvider(
    LocalDownloadModel provides DownloadModel(this)
  ) {
    Box(modifier = with(windowRenderScope) {
      Modifier
        .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
        .scale(scale)
    }) {
      DownloadView()
    }
  }
}