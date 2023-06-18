package org.dweb_browser.dwebview.base

import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.dwebview.DWebView

data class ViewItem(
  val webviewId: String,
  val webView: DWebView,
  val state: WebViewState,
  val navigator: WebViewNavigator,
  val coroutineScope: CoroutineScope,
  var hidden: Boolean = false
)