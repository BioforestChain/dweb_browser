package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi


@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun IDWebView.Render(modifier: Modifier) {
  require(this is DWebView)
  val webView = engine
  UIKitView(factory = { webView }, modifier, update = {})
}