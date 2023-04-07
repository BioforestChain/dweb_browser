package info.bagen.rust.plaoc.ui.browser

import android.annotation.SuppressLint
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import com.google.accompanist.web.*
import info.bagen.rust.plaoc.ui.view.Captureable
import info.bagen.rust.plaoc.ui.view.rememberCaptureController

@SuppressLint("RememberReturnType")
@Composable
fun BrowserWebView(
  viewModel: BrowserViewModel,
  browserWebView: BrowserWebView,
) {
  val localFocusManager = LocalFocusManager.current
  val controller = rememberCaptureController()

  Captureable(
    controller = controller,
    onCaptured = { imageBitmap, throwable ->
      imageBitmap?.let {
        viewModel.uiState.currentBrowserBaseView.value.bitmap = it
      }
    }) {
    WebView(
      state = browserWebView.state,
      navigator = browserWebView.navigator,
//    captureBackPresses = true,
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