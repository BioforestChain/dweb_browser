package info.bagen.dwebbrowser.microService.browser.jmm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.jmm.render.MALLBrowserView
import info.bagen.dwebbrowser.microService.browser.jmm.ui.LocalJmmViewHelper
import kotlinx.coroutines.launch
import org.dweb_browser.window.core.WindowRenderScope
import org.dweb_browser.window.render.LocalWindowController

@Composable
fun JmmController.Render(modifier: Modifier, renderScope: WindowRenderScope) {
  val jmmController = this
  val scope = rememberCoroutineScope()
  val win = LocalWindowController.current
  win.state.title = this.viewModel.uiState.jmmAppInstallManifest.name
  win.GoBackHandler {
    scope.launch { jmmController.closeSelf() }
  }
  CompositionLocalProvider(
    LocalJmmViewHelper provides jmmController.viewModel
  ) {
    Box(modifier = modifier) {
      Box(modifier = with(renderScope) {
        Modifier
          .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
          .scale(scale)
      }) {
        MALLBrowserView {
          scope.launch { jmmController.closeSelf() }
        }
      }
    }
  }


}