package org.dweb_browser.browser.web.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.toWebColorScheme
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.browser.web.model.BrowserWebView
import org.dweb_browser.browser.web.model.WebSiteType
import org.dweb_browser.browser.web.ui.browser.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.browser.model.LocalWebViewInitialScale
import org.dweb_browser.browser.web.ui.browser.model.toWebSiteInfo
import org.dweb_browser.browser.web.ui.loading.LoadingView
import org.dweb_browser.dwebview.Render
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.watchedState

@Composable
internal fun BrowserWebView(viewModel: BrowserViewModel, browserWebView: BrowserWebView) {
  var webViewY = 0 // 用于截图的时候进行定位截图
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
            browserWebView.controller.capture()
          }

          else -> {
            browserWebView.loadState.value = true
          }
        }
      }
    }
    onDispose { job.cancel() }
  }

  // 未解决
  /*LaunchedEffect(browserWebView.controller) {
    browserWebView.controller.captureRequests.mapNotNull {
      delay(500)
      browserWebView.viewItem.webView.asAndroidWebView().drawToBitmapPostLaidOut(webViewY)
    }.onEach {
      it.first?.let { bitmap ->
        browserWebView.bitmap = bitmap.asImageBitmap()
      }
    }.launchIn(this)
  }*/

  val background = MaterialTheme.colorScheme.background

  val win = LocalWindowController.current
  val colorScheme by win.watchedState { colorScheme }
  LaunchedEffect(colorScheme) {
    browserWebView.viewItem.webView.setPrefersColorScheme(colorScheme.toWebColorScheme())
  }

  val initialScale = LocalWebViewInitialScale.current

  LaunchedEffect(initialScale) {
    browserWebView.viewItem.webView.setContentScale(initialScale)
  }
  browserWebView.viewItem.webView.Render(
    modifier = Modifier
      .fillMaxSize()
      .background(background),
    /*onCreate = {
      // 未解决
          val androidView = browserWebView.viewItem.webView.asAndroidWebView()
          androidView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
              browserWebView.controller.capture()
            }
            false
          }
          androidView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            webViewY = scrollY // 用于截图的时候进行定位截图
      //      if (scrollY == 0 || oldScrollY == 0) return@setOnScrollChangeListener
      //      localFocusManager.clearFocus() // TODO 清除焦点
      //      if (oldScrollY < scrollY - 5) {
      //        viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(false)) // TODO 上滑，需要隐藏底部栏
      //      } else if (oldScrollY > scrollY + 5) {
      //        viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(true))  // TODO 下滑，需要显示底部栏
      //      }
          }
    }*/
  )
  LoadingView(browserWebView.loadState) // 先不显示加载框。
}
