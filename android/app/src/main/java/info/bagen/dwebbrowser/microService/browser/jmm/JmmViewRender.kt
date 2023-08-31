package info.bagen.dwebbrowser.microService.browser.jmm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.jmm.render.MALLBrowserView
import kotlinx.coroutines.launch
import org.dweb_browser.window.core.WindowRenderScope

@Composable
fun JmmController.Render(modifier: Modifier, renderScope: WindowRenderScope) {
  val jmmController = this
  val scope = rememberCoroutineScope()
  Box(modifier = modifier) {
    Box(
      modifier = with(renderScope) {
        Modifier
          .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
          .scale(scale)
      }
    ) {
      MALLBrowserView(viewModel = jmmController.viewModel) {
        scope.launch { jmmController.closeSelf() }
      }
    }
  }
}