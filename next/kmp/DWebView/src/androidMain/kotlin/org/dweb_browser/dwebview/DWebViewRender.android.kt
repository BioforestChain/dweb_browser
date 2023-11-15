package org.dweb_browser.dwebview

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import kotlinx.coroutines.launch

@Composable
actual fun IDWebView.Render(
  modifier: Modifier,
  onCreate: (suspend IDWebView.() -> Unit)?,
  onDispose: (suspend IDWebView.() -> Unit)?,
) {
  require(this is DWebView)
  val webView = engine
  val state = rememberWebViewState(getUrl())
  val navigator = rememberWebViewNavigator(webView.scope)
  val client = remember { AccompanistWebViewClient() }
  val chromeClient = remember { AccompanistWebChromeClient() }
  WebView(
    state = state,
    navigator = navigator,
    modifier = Modifier.fillMaxSize(),
    factory = {
      // 修复 activity 已存在父级时导致的异常
      webView.parent?.let { parentView ->
        (parentView as ViewGroup).removeAllViews()
      }
      webView.setBackgroundColor(Color.Transparent.toArgb())
      // viewItem.webView.activity.resources.obtainAttributes() androidx.appcompat.R.attr.colorAccent
      // viewItem.webView.resources.newTheme().obtainStyledAttributes(textColorHighlight )
      webView
    },
    onCreated = {
      engine.scope.launch {
        onCreate?.invoke(this@Render)
      }
    },
    client = client,
    chromeClient = chromeClient,
    captureBackPresses = false,
  )
  DisposableEffect(this) {
    onDispose {
      engine.scope.launch {
        onDispose?.invoke(this@Render)
      }
    }
  }
  BeforeUnloadDialog()
}