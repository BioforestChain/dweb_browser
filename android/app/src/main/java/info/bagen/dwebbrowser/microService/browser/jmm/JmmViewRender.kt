package info.bagen.dwebbrowser.microService.browser.jmm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.jmm.ui.MALLBrowserView

@Composable
fun JmmController.Render(modifier: Modifier, width: Float, height: Float, scale: Float) {
  val jmmController = this
  Box(modifier = modifier) {
    Box(
      modifier = Modifier
        .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
        .scale(scale)
    ) {
      MALLBrowserView(viewModel = jmmController.viewModel) {
        // 关闭
      }
    }
  }
}