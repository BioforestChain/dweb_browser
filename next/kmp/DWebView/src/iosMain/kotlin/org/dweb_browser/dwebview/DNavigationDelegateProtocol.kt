package org.dweb_browser.dwebview

import platform.Foundation.NSError
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.WebKit.WKDownload
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKWebView
import platform.WebKit.WKWebpagePreferences

data class DNavigationDelegateProtocol(
  val didFailNavigation: (SuperCaller.(webView: WKWebView, navigation: WKNavigation?, withError: NSError) -> Unit)? = null,
  val didFinishNavigation: (SuperCaller.(webView: WKWebView, navigation: WKNavigation?) -> Unit)? = null,
  val authenticationChallenge: (SuperCaller.(
    webView: WKWebView,
    authenticationChallenge: NSURLAuthenticationChallenge, shouldAllowDeprecatedTLS: (Boolean) -> Unit
  ) -> Unit)? = null,
  val decidePolicyForNavigationAction: (SuperCaller.(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction, decisionHandler: (WKNavigationActionPolicy) -> Unit
  ) -> Unit)? = null,

  val decidePolicyForNavigationResponse: (SuperCaller.(
    webView: WKWebView,
    decidePolicyForNavigationResponse: WKNavigationResponse, decisionHandler: (WKNavigationResponsePolicy) -> Unit
  ) -> Unit)? = null,

  val didReceiveAuthenticationChallenge: (SuperCaller.(
    webView: WKWebView,
    didReceiveAuthenticationChallenge: NSURLAuthenticationChallenge, completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
  ) -> Unit)? = null,


  val navigationAction: (SuperCaller.(
    webView: WKWebView,
    navigationAction: WKNavigationAction, didBecomeDownload: WKDownload
  ) -> Unit)? = null,

  val navigationResponse: (SuperCaller.(
    webView: WKWebView,
    navigationResponse: WKNavigationResponse, didBecomeDownload: WKDownload
  ) -> Unit)? = null,

  val decidePolicyForNavigationActionPreferences: (SuperCaller.(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction, preferences: WKWebpagePreferences, decisionHandler: (WKNavigationActionPolicy, WKWebpagePreferences?) -> Unit
  ) -> Unit)? = null,
  val webViewWebContentProcessDidTerminate: (SuperCaller.(webView: WKWebView) -> Unit)? = null,
) {
  class SuperCaller(private val caller: () -> Unit) {
    operator fun invoke() = caller()
  }
}