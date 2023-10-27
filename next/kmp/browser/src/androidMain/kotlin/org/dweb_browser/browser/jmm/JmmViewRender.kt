package org.dweb_browser.browser.jmm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.jmm.render.BottomDownloadButton
import org.dweb_browser.browser.jmm.render.WebviewVersionWarningDialog
import org.dweb_browser.browser.jmm.render.ImagePreview
import org.dweb_browser.browser.jmm.render.PreviewState
import org.dweb_browser.browser.jmm.render.JmmAppInstallManifest.Render
import org.dweb_browser.browser.jmm.render.measureCenterOffset
import org.dweb_browser.browser.jmm.ui.LocalJmmViewHelper
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun JmmInstallerController.Render(modifier: Modifier, renderScope: WindowRenderScope) {
  val win = LocalWindowController.current
  win.state.title = this.viewModel.uiState.jmmAppInstallManifest.name
  win.GoBackHandler {
    closeSelf()
  }

  CompositionLocalProvider(LocalJmmViewHelper provides viewModel) {
    Box(modifier = with(renderScope) {
      modifier
        .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
        .scale(scale)
    }) {
      val jmmMetadata = viewModel.uiState.jmmAppInstallManifest

      val lazyListState = rememberLazyListState()
      val screenWidth = LocalConfiguration.current.screenWidthDp
      val screenHeight = LocalConfiguration.current.screenHeightDp
      val density = LocalDensity.current.density
      val statusBarHeight = WindowInsets.statusBars.getTop(LocalDensity.current)
      val previewState = remember {
        PreviewState(
          outsideLazy = lazyListState,
          screenWidth = screenWidth,
          screenHeight = screenHeight,
          statusBarHeight = statusBarHeight,
          density = density
        )
      }
      jmmMetadata.Render { index, imageLazyListState ->
        previewState.selectIndex.value = index
        previewState.imageLazy = imageLazyListState
        previewState.offset.value = measureCenterOffset(index, previewState)
        previewState.showPreview.targetState = true
      }
      BottomDownloadButton()
      ImagePreview(jmmMetadata, previewState)
      WebviewVersionWarningDialog()
    }
  }
}