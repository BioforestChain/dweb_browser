package org.dweb_browser.sys.keychain.render


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.compose.ListDetailPaneScaffold
import org.dweb_browser.helper.compose.rememberListDetailPaneScaffoldNavigator
import org.dweb_browser.sys.keychain.KeychainI18nResource
import org.dweb_browser.sys.keychain.KeychainManager
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowSurface
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.core.LocalWindowController


@Composable
fun KeychainManager.Render(
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope,
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
      ListView(Modifier, WindowContentRenderScope.Unspecified)
    },
    detailPane = {
      when (val detail = detailController) {
        null -> WindowContentRenderScope.Unspecified.WindowSurface {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(KeychainI18nResource.no_select_detail())
          }
        }

        else -> BoxWithConstraints {
          detail.Render(
            Modifier.fillMaxSize(), WindowContentRenderScope.Unspecified
          )
        }
      }
    }
  )
}

