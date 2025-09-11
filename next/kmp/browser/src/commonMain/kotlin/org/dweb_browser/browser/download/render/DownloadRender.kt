package org.dweb_browser.browser.download.render

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadController
import org.dweb_browser.browser.download.DownloadI18n
import org.dweb_browser.browser.download.model.DownloadTask
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.WindowSurface
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.core.LocalWindowController

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DownloadController.Render(modifier: Modifier, windowRenderScope: WindowContentRenderScope) {
  val navigator = rememberListDetailPaneScaffoldNavigator<DownloadTask>()
  val win = LocalWindowController.current
  win.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.navigateBack()
  }

  val isListAndDetailVisible =
    navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded && navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

  val scope = rememberCoroutineScope()

  SharedTransitionLayout {
    AnimatedContent(isListAndDetailVisible, label = "Download Manager") {
      ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = modifier.withRenderScope(windowRenderScope),
        listPane = {
          AnimatedPane {
            WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
              modifier = Modifier.fillMaxSize(),
              topBarTitleText = BrowserI18nResource.top_bar_title_download(),
            ) { innerPadding ->
              DownloadList(Modifier.padding(innerPadding)) { downloadTask ->
                scope.launch {
                  navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, downloadTask)
                }
              }
            }
          }
        },
        detailPane = {
          AnimatedPane {
            when (val downloadTask = navigator.currentDestination?.contentKey) {
              null -> WindowContentRenderScope.Unspecified.WindowSurface {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(DownloadI18n.no_select_detail())
                }
              }

              else -> WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
                modifier = Modifier.fillMaxSize(),
                topBarTitleText = BrowserI18nResource.top_bar_title_down_detail(),
              ) { innerPadding ->
                decompressModel.Render(downloadTask, Modifier.padding(innerPadding))
              }
            }
          }
        }
      )
    }
  }
}