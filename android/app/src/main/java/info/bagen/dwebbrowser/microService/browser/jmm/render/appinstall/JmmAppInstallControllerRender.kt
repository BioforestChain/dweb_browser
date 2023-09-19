package info.bagen.dwebbrowser.microService.browser.jmm.render.appinstall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.navigation.compose.rememberNavController
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.JmmAppInstallController
import kotlinx.coroutines.launch
import org.dweb_browser.window.render.LocalWindowController

@Composable
fun JmmAppInstallController.Render() {
  val jmmMetadata = uiState.jmmAppInstallManifest
  val lazyListState = rememberLazyListState()
  val screenWidth = LocalConfiguration.current.screenWidthDp
  val screenHeight = LocalConfiguration.current.screenHeightDp
  val density = LocalContext.current.resources.displayMetrics.density
  val statusBarHeight = WindowInsets.statusBars.getTop(Density(App.appContext))
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

  val win = LocalWindowController.current
  SideEffect {
    win.state.title = jmmMetadata.name
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
    //.navigationBarsPadding()
  ) {
    AppInfoContentView(jmmMetadata) { index, imageLazyListState ->
      scope.launch {
        previewState.selectIndex.value = index
        previewState.imageLazy = imageLazyListState
        previewState.offset.value = measureCenterOffset(index, previewState)
        previewState.showPreview.targetState = true
      }
    }
    BottomDownloadButton(
      modifier = Modifier.align(Alignment.BottomCenter), uiState
    ) {
      scope.launch {
        doDownload()
      }
    }
    ImagePreview(jmmMetadata, previewState)
    DialogForWebviewVersion()
  }
}