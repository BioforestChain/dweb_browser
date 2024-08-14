package org.dweb_browser.dwebview

import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
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
  val focusRequester = LocalFocusRequester.current

  AndroidView(
    factory = {
      // 修复 activity 已存在父级时导致的异常
      webView.parent?.let { parentView ->
        (parentView as ViewGroup).removeAllViews()
      }
      webView.setBackgroundColor(Color.Transparent.toArgb())
      webView.layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
      )
      onCreate?.also {
        webView.lifecycleScope.launch { onCreate.invoke(this@Render) }
      }
      webView
    },
    modifier = when (focusRequester) {
      null -> modifier
      else -> modifier
        .focusRequester(focusRequester)
        .onFocusChanged {
          if (it.isFocused) {
            webView.requestFocus()
          }
        }
        .focusable()
    },
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
        engine.lifecycleScope.launch {
          onDispose()
        }
      }
    }
  }

  BeforeUnloadDialog()
}