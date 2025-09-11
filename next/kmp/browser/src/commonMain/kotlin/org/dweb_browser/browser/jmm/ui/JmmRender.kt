package org.dweb_browser.browser.jmm.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.dweb_browser.browser.jmm.JmmI18nResource
import org.dweb_browser.browser.jmm.JmmMetadata
import org.dweb_browser.browser.jmm.JmmRenderController
import org.dweb_browser.browser.jmm.JmmTabs
import org.dweb_browser.sys.window.core.LocalWindowController
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.WindowSurface
import org.dweb_browser.sys.window.core.withRenderScope


@Composable
expect fun JmmRenderController.Render(
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope,
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun JmmRenderController.CommonRender(
  modifier: Modifier, windowRenderScope: WindowContentRenderScope,
) {
  val navigator = rememberListDetailPaneScaffoldNavigator<JmmMetadata>()
  val isListAndDetailVisible =
    navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded && navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
  val win = LocalWindowController.current
  val scope = rememberCoroutineScope()

  win.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.navigateBack()
  }

  /// 外部打开应用详情需要，例如桌面上点击应用详情时
  DisposableEffect(outerHistoryJmmMetadata) {
    if(outerHistoryJmmMetadata != null) {
      scope.launch {
        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, outerHistoryJmmMetadata)
      }
    }
    onDispose {
      outerHistoryJmmMetadata = null
    }
  }

  SharedTransitionLayout {
    AnimatedContent(targetState = isListAndDetailVisible, label = "Download Manager") {
      ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
          var curTab by remember { mutableStateOf(JmmTabs.Installed) }

          AnimatedPane {
            WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
              modifier = Modifier.fillMaxSize(),
              topBarTitleText = JmmI18nResource.top_bar_title_install(),
            ) { innerPadding ->
              JmmListView(
                modifier = Modifier.padding(innerPadding),
                curTab = curTab,
                onTabClick = { selectedTab ->
                  curTab = selectedTab
                },
                onOpenDetail = { jmmMetadata ->
                  scope.launch {
                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, jmmMetadata)
                  }
                }
              )
            }
          }
        },
        detailPane = {
          AnimatedPane {
            when (val jmmMetadata = navigator.currentDestination?.contentKey) {
              null -> WindowContentRenderScope.Unspecified.WindowSurface {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(JmmI18nResource.no_select_detail())
                }
              }

              else -> {
                BoxWithConstraints {
                  getJmmDetailController(jmmMetadata).Render(
                    Modifier.fillMaxSize(), WindowContentRenderScope.Unspecified
                  )
                }
              }
            }
          }
        },
        modifier = modifier.withRenderScope(windowRenderScope),
      )
    }
  }
}
