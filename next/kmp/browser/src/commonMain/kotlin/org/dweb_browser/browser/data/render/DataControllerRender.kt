package org.dweb_browser.browser.data.render

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
import org.dweb_browser.browser.data.DataController
import org.dweb_browser.browser.data.DataI18n
import org.dweb_browser.helper.compose.NoDataRender
import org.dweb_browser.sys.window.core.LocalWindowController
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.WindowSurface
import org.dweb_browser.sys.window.core.withRenderScope

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DataController.Render(modifier: Modifier, windowRenderScope: WindowContentRenderScope) {
  val navigator = rememberListDetailPaneScaffoldNavigator<DataController.ProfileDetail>()
  LocalWindowController.current.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.navigateBack()
  }

  val isListAndDetailVisible =
    navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded && navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

  val scope = rememberCoroutineScope()

  SharedTransitionLayout {
    AnimatedContent(isListAndDetailVisible, label = "Data Manager") {
      ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = modifier.withRenderScope(windowRenderScope),
        listPane = {
          AnimatedPane {
            ListRender { profileDetail ->
              scope.launch {
                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, profileDetail)
              }
            }
          }
        },
        detailPane = {
          AnimatedPane {
            when (val profileDetail = navigator.currentDestination?.contentKey) {
              null -> WindowContentRenderScope.Unspecified.WindowSurface {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(DataI18n.select_profile_for_detail_view())
                }
              }

              else -> WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
                Modifier.fillMaxSize(), topBarTitleText = profileDetail.short_name
              ) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                  NoDataRender(DataI18n.no_support_detail_view())
                }
              }
            }
          }
        },
      )
    }
  }

  DeleteDialogRender()
}
