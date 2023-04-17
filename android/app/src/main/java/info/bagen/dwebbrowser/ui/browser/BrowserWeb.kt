package info.bagen.dwebbrowser.ui.browser

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalFocusManager
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import info.bagen.dwebbrowser.ui.view.Captureable
import kotlinx.coroutines.delay

@Composable
fun BrowserWebView(
  viewModel: BrowserViewModel,
  browserWebView: BrowserWebView,
) {
  val localFocusManager = LocalFocusManager.current

  LaunchedEffect(browserWebView.webView) {
    snapshotFlow { !browserWebView.state.isLoading }.collect {
      if (it) {
        delay(1000)
        browserWebView.controller.capture()
      }
    }
  }
  LaunchedEffect(browserWebView.state) {
    snapshotFlow { browserWebView.state.loadingState }.collect {
      if (it is LoadingState.Loading) browserWebView.showBottomBar.targetState = true
    }
  }

  Captureable(
    controller = browserWebView.controller,
    onCaptured = { imageBitmap, throwable ->
      imageBitmap?.let { bitmap ->
        viewModel.uiState.currentBrowserBaseView.value.bitmap = bitmap
      }
    }) {
    WebView(
      state = browserWebView.state,
      navigator = browserWebView.navigator,
      factory = {
        browserWebView.webView.parent?.let { (it as ViewGroup).removeAllViews() }
        browserWebView.webView.apply {
          setOnScrollChangeListener { view, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY == 0 || oldScrollY == 0) return@setOnScrollChangeListener
            localFocusManager.clearFocus() // TODO 清除焦点
            if (oldScrollY < scrollY - 5 && browserWebView.showBottomBar.currentState) {
              viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(false)) // TODO 上滑，需要隐藏底部栏
            } else if (oldScrollY > scrollY + 5 && !browserWebView.showBottomBar.currentState) {
              viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(true))  // TODO 下滑，需要显示底部栏
            }
          }
        }
        browserWebView.webView
      }
    )
  }
}