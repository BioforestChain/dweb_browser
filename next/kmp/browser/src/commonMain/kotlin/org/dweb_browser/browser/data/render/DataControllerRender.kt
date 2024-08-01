package org.dweb_browser.browser.data.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.data.DataController
import org.dweb_browser.helper.compose.ListDetailPaneScaffold
import org.dweb_browser.helper.compose.collectAsMutableState
import org.dweb_browser.helper.compose.rememberListDetailPaneScaffoldNavigator
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun DataController.Render(modifier: Modifier, windowRenderScope: WindowContentRenderScope) {
  val navigator = rememberListDetailPaneScaffoldNavigator()
  var selectedProfileDetail by profileDetailFlow.collectAsMutableState()
  remember(selectedProfileDetail) {
    when (selectedProfileDetail) {
      null -> navigator.backToList()
      else -> navigator.navigateToDetail { selectedProfileDetail = null }
    }
  }
  LocalWindowController.current.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.backToList {
      selectedProfileDetail = null
    }
  }

  ListDetailPaneScaffold(
    navigator = navigator,
    modifier = modifier.withRenderScope(windowRenderScope),
    listPane = { ListRender() },
    detailPane = { DetailRender() },
  )
  DeleteDialogRender()
}
