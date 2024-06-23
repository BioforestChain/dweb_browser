package org.dweb_browser.browser.download

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.download.model.LocalDownloadModel
import org.dweb_browser.browser.download.ui.DownloadView
import org.dweb_browser.browser.download.ui.LocalDecompressModel
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText

@Composable
fun DownloadController.Render(modifier: Modifier, windowRenderScope: WindowContentRenderScope) {
  LocalCompositionChain.current.Provider(
    LocalDownloadModel provides this.downloadModel,
    LocalDecompressModel provides this.decompressModel,
  ) {
    windowRenderScope.WindowContentScaffoldWithTitleText(modifier, topBarTitleText = "") { it ->
      DownloadView(Modifier.padding(it))
    }
  }
}