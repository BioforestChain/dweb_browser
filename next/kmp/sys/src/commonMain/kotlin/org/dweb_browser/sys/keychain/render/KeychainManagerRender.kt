package org.dweb_browser.sys.keychain.render


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.sys.keychain.KeychainI18nResource
import org.dweb_browser.sys.keychain.KeychainManager
import org.dweb_browser.sys.window.core.LocalWindowController
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowSurface
import org.dweb_browser.sys.window.core.withRenderScope


@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun KeychainManager.Render(
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope,
) {
  val navigator = rememberListDetailPaneScaffoldNavigator<IMicroModuleManifest>()
  val win = LocalWindowController.current
  win.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.navigateBack()
  }

  val isListAndDetailVisible =
    navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded && navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

  val scope = rememberCoroutineScope()

  SharedTransitionLayout {
    AnimatedContent(isListAndDetailVisible, label = "Keychain Manager") {
      ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = modifier.withRenderScope(windowRenderScope),
        listPane = {
          AnimatedPane {
            ListView(Modifier, WindowContentRenderScope.Unspecified) { manifest ->
              scope.launch {
                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, manifest)
              }
            }
          }
        },
        detailPane = {
          AnimatedPane {
            when (val manifest = navigator.currentDestination?.contentKey) {
              null -> WindowContentRenderScope.Unspecified.WindowSurface {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(KeychainI18nResource.no_select_detail())
                }
              }

              else -> BoxWithConstraints {
                getDetailManager(manifest).Render(
                  Modifier.fillMaxSize(), WindowContentRenderScope.Unspecified
                )
              }
            }
          }
        },
      )
    }
  }
}

