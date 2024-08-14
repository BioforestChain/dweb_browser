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
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.WindowControllerBinding
import org.dweb_browser.dwebview.RenderWithScale
import org.dweb_browser.dwebview.rememberCanGoBack
import org.dweb_browser.dwebview.rememberCanGoForward
import org.dweb_browser.sys.window.render.watchedIsMaximized

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
    list.lastOrNull()?.also { LatestEffect(it, list) }
  }
}

@Composable
private fun MultiWebViewController.LatestEffect(
  viewItem: MultiWebViewController.MultiViewItem,
  list: List<MultiWebViewController.MultiViewItem>,
) {
  val webView = viewItem.webView
  /// 返回按钮的拦截只跟最后一个视图有关系，直到这最后一个视图被关闭了
  if (viewItem == list.last()) {
    val isMaximized by win.watchedIsMaximized()
    // 在 MWebView 的全屏窗口中，默认将返回按钮的行为与应用退出关联在一起
    if (win.state.canGoForward != null) {
      win.state.canGoForward = webView.rememberCanGoForward()
    }
    val canGoBack =
      if (isMaximized) true else webView.closeWatcher.canClose || webView.rememberCanGoBack()
    win.navigation.GoBackHandler(canGoBack) {
      if (webView.canGoBack()) {
        webView.goBack()
      } else if (list.size > 1) {
        viewItem.coroutineScope.launch {
          closeWebView(viewItem.webviewId)
        }
      }
    }
  }
}
