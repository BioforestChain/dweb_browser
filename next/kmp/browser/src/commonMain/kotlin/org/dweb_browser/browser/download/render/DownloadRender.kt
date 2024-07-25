package org.dweb_browser.browser.download.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadController
import org.dweb_browser.browser.download.DownloadI18n
import org.dweb_browser.helper.compose.ListDetailPaneScaffold
import org.dweb_browser.helper.compose.rememberListDetailPaneScaffoldNavigator
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.WindowSurface
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun DownloadController.Render(modifier: Modifier, windowRenderScope: WindowContentRenderScope) {
  val navigator = rememberListDetailPaneScaffoldNavigator()
  val win = LocalWindowController.current
  win.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.backToList {
      decompressModel.hide()
    }
  }
  ListDetailPaneScaffold(
    navigator,
    modifier = modifier.withRenderScope(windowRenderScope),
    listPane = {
      WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
        modifier = Modifier.fillMaxSize(),
        topBarTitleText = BrowserI18nResource.top_bar_title_download(),
      ) { innerPadding ->
        DownloadList(navigator, Modifier.padding(innerPadding))
      }
    },
    detailPane = {
      when (val downloadTask = decompressModel.downloadTask) {
        null -> WindowContentRenderScope.Unspecified.WindowSurface {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(DownloadI18n.no_select_detail())
          }
        }

        else -> WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
          modifier = Modifier.fillMaxSize(),
          topBarTitleText = BrowserI18nResource.top_bar_title_down_detail(),
        ) { innerPadding ->
          LocalWindowController.current.navigation.GoBackHandler {
            navigator.backToList()
          }
          decompressModel.Render(downloadTask, Modifier.padding(innerPadding))
        }
      }
    }
  )
}