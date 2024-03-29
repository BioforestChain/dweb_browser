package org.dweb_browser.dwebview

import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberSaveableWebViewState
import com.google.accompanist.web.rememberWebViewNavigator
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.LocalFocusRequester

@Composable
actual fun IDWebView.Render(
  modifier: Modifier,
  onCreate: (suspend IDWebView.() -> Unit)?,
  onDispose: (suspend IDWebView.() -> Unit)?,
) {
  require(this is DWebView)
  val webView = engine
  // val state = rememberWebViewState(webView.url ?: "about:blank")
  val state = rememberSaveableWebViewState()
  val navigator = rememberWebViewNavigator(webView.ioScope)
  val client = remember { AccompanistWebViewClient() }
  val chromeClient = remember { AccompanistWebChromeClient() }
  val focusRequester = LocalFocusRequester.current
  BoxWithConstraints(
    modifier = when (focusRequester) {
      null -> Modifier
      else -> Modifier.focusRequester(focusRequester).onFocusChanged {
        if (it.isFocused) {
          webView.requestFocus()
        }
      }.focusable()
    }
  ) {
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
    /// 处理键盘响应
    val imm = remember(webView) {
      webView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    DisposableEffect(webView) {
      webView.setOnFocusChangeListener { v, hasFocus ->
        if (hasFocus) {
          imm.hideSoftInputFromWindow(webView.windowToken, 0)
        }
      }
      onDispose {
        webView.onFocusChangeListener = null
      }
    }
    /// 生命周期
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