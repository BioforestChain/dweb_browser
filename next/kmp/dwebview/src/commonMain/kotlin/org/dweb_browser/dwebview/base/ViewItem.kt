package org.dweb_browser.dwebview.base

import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.dwebview.IDWebView

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
  val webView: IDWebView
  val coroutineScope: CoroutineScope
  var hidden: Boolean
}
