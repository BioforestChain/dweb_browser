package info.bagen.dwebbrowser.microService.browser.jmm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.browser.jmm.ui.MALLBrowserView

@Composable
fun JmmController.Render(scaleX: Float, scaleY: Float) {
  val jmmController = this
  val screenSize = LocalConfiguration.current.let {
    Pair(it.screenWidthDp.dp, it.screenHeightDp.dp)
  }
  Box(
    modifier = Modifier
      .requiredSize(width = screenSize.first, height = screenSize.second) // 原始大小
      .scale(scaleX, scaleY)
  ) {
    MALLBrowserView(viewModel = jmmController.viewModel) {
      // 关闭
    }
  }
}