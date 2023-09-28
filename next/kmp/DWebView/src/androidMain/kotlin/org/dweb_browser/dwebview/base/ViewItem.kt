package org.dweb_browser.dwebview.base

import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.dwebview.DWebViewEngine

/*data class ViewItem(
  val webviewId: String,
  val webView: DWebView,
  val state: WebViewState,
  val navigator: WebViewNavigator,
  val coroutineScope: CoroutineScope,
  var hidden: Boolean = false
)*/

interface ViewItem {
  val webviewId: String
  val webView: DWebViewEngine
  val state: WebViewState
  val navigator: WebViewNavigator
  val coroutineScope: CoroutineScope
  var hidden: Boolean
}

data class DWebViewItem(
  override val webviewId: String,
  override val webView: DWebViewEngine,
  override val state: WebViewState,
  override val navigator: WebViewNavigator,
  override val coroutineScope: CoroutineScope,
  override var hidden: Boolean = false,
) : ViewItem