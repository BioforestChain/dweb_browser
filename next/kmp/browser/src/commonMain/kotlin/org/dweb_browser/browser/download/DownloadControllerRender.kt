package org.dweb_browser.browser.download

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.model.LocalDownloadModel
import org.dweb_browser.browser.download.render.DecompressView
import org.dweb_browser.browser.download.render.DownloadHistory
import org.dweb_browser.browser.download.render.LocalDecompressModel
import org.dweb_browser.helper.compose.ListDetailPaneScaffold
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.rememberListDetailPaneScaffoldNavigator
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun DownloadController.Render(modifier: Modifier, windowRenderScope: WindowContentRenderScope) {
  LocalCompositionChain.current.Provider(
    LocalDownloadModel provides downloadModel,
    LocalDecompressModel provides decompressModel,
  ) {
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
        val scope = rememberCoroutineScope()
        WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
          modifier = Modifier.fillMaxSize(),
          topBarTitleText = BrowserI18nResource.top_bar_title_download(),
          topBarActions = {
            scope.launch { downloadModel.close() }
          }
        ) { innerPadding ->
          DownloadHistory(navigator, Modifier.padding(innerPadding))
        }
      },
      detailPane = {
        WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
          modifier = Modifier.fillMaxSize(),
          topBarTitleText = BrowserI18nResource.top_bar_title_down_detail(),
          topBarActions = {
            decompressModel.hide()
          }
        ) { innerPadding ->
          DecompressView(Modifier.padding(innerPadding))
        }
      }
    )
  }
}