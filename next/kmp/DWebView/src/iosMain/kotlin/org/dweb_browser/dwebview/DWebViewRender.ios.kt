package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch


@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun IDWebView.Render(
  modifier: Modifier,
  onCreate: (suspend IDWebView.() -> Unit)?,
  onDispose: (suspend IDWebView.() -> Unit)?,
) {
  require(this is DWebView)
  val webView = engine
  UIKitView(factory = {
    engine.scope.launch {
      onCreate?.invoke(this@Render);
    }
    webView
  }, modifier, update = {}, onRelease = {
    engine.scope.launch {
      onDispose?.invoke(this@Render);
    }
  })
}