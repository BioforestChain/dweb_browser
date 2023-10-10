package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import org.dweb_browser.browser.jmm.ui.LocalJmmViewHelper

@Composable
fun MALLBrowserView(onBack: () -> Unit) {
  val jmmMetadata = LocalJmmViewHelper.current.uiState.jmmAppInstallManifest
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
  val scope = rememberCoroutineScope()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    AppInfoContentView(lazyListState, jmmMetadata) { index, imageLazyListState ->
      scope.launch {
        previewState.selectIndex.value = index
        previewState.imageLazy = imageLazyListState
        previewState.offset.value = measureCenterOffset(index, previewState)
        previewState.showPreview.targetState = true
      }
    }
    BottomDownloadButton()
    ImagePreview(jmmMetadata, previewState)
    DialogForWebviewVersion()
  }
}