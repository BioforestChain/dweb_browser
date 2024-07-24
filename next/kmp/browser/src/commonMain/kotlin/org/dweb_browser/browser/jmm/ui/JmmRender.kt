package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmI18nResource
import org.dweb_browser.browser.jmm.JmmRenderController
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.helper.compose.ListDetailPaneScaffold
import org.dweb_browser.helper.compose.rememberListDetailPaneScaffoldNavigator
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.WindowSurface
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController


@Composable
expect fun JmmRenderController.Render(
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope,
)

@Composable
internal fun JmmRenderController.CommonRender(
  modifier: Modifier, windowRenderScope: WindowContentRenderScope,
) {
  val navigator = rememberListDetailPaneScaffoldNavigator()
  val win = LocalWindowController.current
  win.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.backToList {
      closeDetail()
    }
  }
  LaunchedEffect(detailController) {
    if (detailController != null) {
      navigator.navigateToDetail()
    } else {
      navigator.backToList()
    }
  }

  ListDetailPaneScaffold(
    modifier = modifier.withRenderScope(windowRenderScope),
    navigator = navigator,
    listPane = {
      WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
        modifier = Modifier.fillMaxSize(),
        topBarTitleText = JmmI18nResource.top_bar_title_install(),
      ) { innerPadding ->
        JmmListView(Modifier.padding(innerPadding))
      }
    },
    detailPane = {
      when (val detail = detailController) {
        null -> WindowContentRenderScope.Unspecified.WindowSurface {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(JmmI18nResource.no_select_detail())
          }
        }

        else -> BoxWithConstraints {
          detail.Render(
            Modifier.fillMaxSize(), WindowContentRenderScope.Unspecified
          )
        }
      }
    },
  )
}

@Composable
internal fun JmmStatus.showText() = when (this) {
  JmmStatus.Downloading -> BrowserI18nResource.install_button_downloading()
  JmmStatus.Paused -> BrowserI18nResource.install_button_paused()
  JmmStatus.Failed -> BrowserI18nResource.install_button_retry2()
  JmmStatus.Init, JmmStatus.Canceled -> BrowserI18nResource.install_button_install()
  JmmStatus.Completed -> BrowserI18nResource.install_button_installing()
  JmmStatus.INSTALLED -> BrowserI18nResource.install_button_open()
  JmmStatus.NewVersion -> BrowserI18nResource.install_button_update()
  JmmStatus.VersionLow -> BrowserI18nResource.install_button_open() // 理论上历史列表应该不存在这状态
}