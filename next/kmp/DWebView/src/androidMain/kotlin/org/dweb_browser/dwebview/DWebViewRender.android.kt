package org.dweb_browser.dwebview

import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
  val navigator = rememberWebViewNavigator(webView.ioScope)
  val client = remember { AccompanistWebViewClient() }
  val chromeClient = remember { AccompanistWebChromeClient() }
  BoxWithConstraints {
    val contentScale by contentScale
    WebView(
      state = state,
      navigator = navigator,
      modifier = modifier.requiredSize(maxWidth / contentScale, maxHeight / contentScale),
      factory = {
        // 修复 activity 已存在父级时导致的异常
        webView.parent?.let { parentView ->
          (parentView as ViewGroup).removeAllViews()
        }
        webView.setBackgroundColor(Color.Transparent.toArgb())
        webView
      },
      onCreated = {
        onCreate?.also {
          engine.ioScope.launch { onCreate.invoke(this@Render) }
        }
      },
      client = client,
      chromeClient = chromeClient,
      captureBackPresses = false,
    )
    onDispose?.also {
      DisposableEffect(this) {
        onDispose {
          engine.ioScope.launch {
            onDispose()
          }
        }
      }
    }
  }
  BeforeUnloadDialog()
}