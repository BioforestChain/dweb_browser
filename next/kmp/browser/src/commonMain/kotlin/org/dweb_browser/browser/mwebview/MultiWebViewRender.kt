package org.dweb_browser.browser.mwebview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.common.WindowControllerBinding
import org.dweb_browser.dwebview.RenderWithScale
import org.dweb_browser.dwebview.rememberCanGoBack
import org.dweb_browser.dwebview.rememberHistoryCanGoForward

@Composable
fun MultiWebViewController.Render(
  modifier: Modifier = Modifier,
  scale: Float,
) {
  var list by remember {
    mutableStateOf(listOf<MultiWebViewController.MultiViewItem>())
  }
  DisposableEffect(webViewList) {
    val off = webViewList.onChange {
      list = it.toList()
      if (list.isEmpty()) {
        win.tryCloseOrHide()
      }
      return@onChange;
    }
    list = webViewList.toList()
    onDispose {
      off()
    }
  }

  Box(modifier) {
    list.forEach { viewItem ->
      key(viewItem.webviewId) {
        viewItem.webView.apply {
          WindowControllerBinding()
          RenderWithScale(scale, Modifier.fillMaxSize())
        }
      }
    }
    /// 如果有多个webview，可以通过返回来关闭最后一个
    win.navigation.GoBackHandler(list.size > 1) {
      list.lastOrNull()?.also {
        closeWebView(it.webviewId)
      }
    }
    list.lastOrNull()?.also {
      val webView = it.webView
//  /// 返回按钮的拦截只跟最后一个视图有关系，直到这最后一个视图被关闭了
//  val isMaximized by win.watchedIsMaximized()
//  // 在 MWebView 的全屏窗口中，默认将返回按钮的行为与应用退出关联在一起
      if (win.state.canGoForward != null) {
        win.state.canGoForward = webView.rememberHistoryCanGoForward()
      }
      /// 如果最后一个 webview 能够 goBack（closeWatcher+historyGoBack），那么返回按钮执行 goBack。
      val canGoBack = webView.rememberCanGoBack()
      win.navigation.GoBackHandler(canGoBack) {
        webView.goBack()
      }
    }
  }
}
