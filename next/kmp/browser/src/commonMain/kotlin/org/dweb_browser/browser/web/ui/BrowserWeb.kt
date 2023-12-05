package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.CaptureParams
import org.dweb_browser.browser.common.CaptureView
import org.dweb_browser.browser.common.loading.LoadingView
import org.dweb_browser.browser.common.toWebColorScheme
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.browser.web.model.BrowserWebView
import org.dweb_browser.browser.web.model.WebSiteType
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.model.toWebSiteInfo
import org.dweb_browser.dwebview.MotionEventAction
import org.dweb_browser.dwebview.Render
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.watchedState

@Composable
internal fun BrowserWebView(
  viewModel: BrowserViewModel, browserWebView: BrowserWebView, windowRenderScope: WindowRenderScope
) {
  val scope = rememberCoroutineScope()
  DisposableEffect(browserWebView.viewItem.webView) { // 点击跳转时，加载状态变化，将底部栏显示
    val job = scope.launch {
      browserWebView.viewItem.webView.loadingProgressFlow.collect {
        when (it) {
          1f -> {
            browserWebView.loadState.value = false
            delay(500)
            viewModel.changeHistoryLink(
              add = browserWebView.viewItem.webView.toWebSiteInfo(WebSiteType.History)
            )
            browserWebView.controller.capture(
              CaptureParams(
                viewType = CaptureParams.ViewType.WebView,
                webView = browserWebView.viewItem.webView
              )
            )
          }

          else -> {
            browserWebView.loadState.value = true
          }
        }
      }
    }
    onDispose { job.cancel() }
  }

  CaptureView(
    modifier = Modifier.fillMaxSize(),
    controller = browserWebView.controller,
    onCaptured = { imageBitmap, throwable ->
      imageBitmap?.let { browserWebView.bitmap = imageBitmap }
    }
  ) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
      val win = LocalWindowController.current
      val colorScheme by win.watchedState { colorScheme }
      LaunchedEffect(colorScheme) {
        browserWebView.viewItem.webView.setPrefersColorScheme(colorScheme.toWebColorScheme())
      }

      val density = LocalDensity.current.density

      LaunchedEffect(windowRenderScope.scale, maxWidth, maxHeight) {
        browserWebView.viewItem.webView.setContentScale(
          windowRenderScope.scale,
          maxWidth.value,
          maxHeight.value,
          density,
        )
        delay(500)
        browserWebView.controller.capture(
          CaptureParams(
            viewType = CaptureParams.ViewType.WebView,
            webView = browserWebView.viewItem.webView
          )
        )
      }

      browserWebView.viewItem.webView.Render(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background),
        onCreate = {
          val webView = browserWebView.viewItem.webView
          var webViewY = 0 // 用于截图的时候进行定位截图
          webView.setOnTouchListener { _, event ->
            if (event == MotionEventAction.ACTION_UP) {
              scope.launch {
                browserWebView.controller.capture(
                  CaptureParams(CaptureParams.ViewType.WebView, webViewY, webView)
                )
              }
            }
            false
          }
          webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            webViewY = scrollY // 用于截图的时候进行定位截图
//          if (scrollY == 0 || oldScrollY == 0) return@setOnScrollChangeListener
//          localFocusManager.clearFocus() // TODO 清除焦点
//          if (oldScrollY < scrollY - 5) {
//            viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(false)) // TODO 上滑，需要隐藏底部栏
//          } else if (oldScrollY > scrollY + 5) {
//            viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(true))  // TODO 下滑，需要显示底部栏
//          }
          }
        }
      )
    }
  }
  LoadingView(browserWebView.loadState) // 先不显示加载框。
}
