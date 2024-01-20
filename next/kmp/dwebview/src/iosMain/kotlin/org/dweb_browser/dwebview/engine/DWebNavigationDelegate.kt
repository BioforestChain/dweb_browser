package org.dweb_browser.dwebview.engine

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.getUIApplication
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.WebBeforeUnloadArgs
import org.dweb_browser.dwebview.WebLoadErrorState
import org.dweb_browser.dwebview.WebLoadStartState
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.dwebview.base.isWebUrlScheme
import platform.Foundation.NSError
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.create
import platform.Foundation.serverTrust
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKWebView
import platform.WebKit.WKWebpagePreferences
import platform.darwin.NSObject

@Suppress("CONFLICTING_OVERLOADS")
class DWebNavigationDelegate(val engine: DWebViewEngine) : NSObject(),
  WKNavigationDelegateProtocol {
  override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
    engine.mainScope.launch {
      engine.closeSignal.emit()
    }
  }

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationResponse: WKNavigationResponse,
    decisionHandler: (WKNavigationResponsePolicy) -> Unit
  ) {
    decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun decidePolicyForNavigationAction(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    decisionHandler: (WKNavigationActionPolicy) -> Unit
  ) {
    val url = decidePolicyForNavigationAction.request.URL
    val scheme = url?.scheme ?: "http"
    if (url != null && !isWebUrlScheme(scheme)) {
      if (scheme == "dweb") {
        engine.ioScope.launch {
          engine.remoteMM.nativeFetch(url.absoluteString!!)
        }
        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        return
      }
      val uiApp = engine.remoteMM.getUIApplication()
      if (uiApp.canOpenURL(url)) {
        uiApp.openURL(url)
        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        return
      }
    }

    var confirmReferred =
      CompletableDeferred(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
    /// navigationAction.navigationType : https://developer.apple.com/documentation/webkit/wknavigationtype/
    if (engine.beforeUnloadSignal.isNotEmpty()) {
      val message = when (decidePolicyForNavigationAction.navigationType) {
        // reload
        3L -> "重新加载此网站？"
        else -> "离开此网站？"
      }
      confirmReferred = CompletableDeferred()
      engine.ioScope.launch {
        val args = WebBeforeUnloadArgs(message)
        engine.beforeUnloadSignal.emit(args)
        confirmReferred.complete(if (args.waitHookResults()) WKNavigationActionPolicy.WKNavigationActionPolicyAllow else WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
      }
    }
    /// decisionHandler
    if (confirmReferred.isCompleted) {
      decisionHandler(confirmReferred.getCompleted())
    } else {
      engine.ioScope.launch {
        decisionHandler(confirmReferred.await())
      }
    }
  }

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    preferences: WKWebpagePreferences,
    decisionHandler: (WKNavigationActionPolicy, WKWebpagePreferences?) -> Unit
  ) {
    decidePolicyForNavigationAction(webView, decidePolicyForNavigationAction) {
      decisionHandler(it, null)
    }
  }

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    decisionHandler: (WKNavigationActionPolicy) -> Unit
  ) {
    decidePolicyForNavigationAction(webView, decidePolicyForNavigationAction, decisionHandler)
  }

  override fun webView(
    webView: WKWebView, didStartProvisionalNavigation: WKNavigation?
  ) {
    val loadedUrl = webView.URL?.absoluteString ?: "about:blank"
    engine.mainScope.launch { engine.loadStateChangeSignal.emit(WebLoadStartState(loadedUrl)) }
  }

  override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
    val loadedUrl = webView.URL?.absoluteString ?: "about:blank"
    engine.mainScope.launch {
      engine.loadStateChangeSignal.emit(WebLoadSuccessState(loadedUrl))
    }
  }

  override fun webView(
    webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError
  ) {
    val currentUrl = webView.URL?.absoluteString ?: "about:blank"
    val errorMessage = "[${withError.code}]$currentUrl\n${withError.description}"
    engine.mainScope.launch {
      engine.loadStateChangeSignal.emit(
        WebLoadErrorState(
          currentUrl,
          errorMessage
        )
      )
    }
  }

//  override fun webView(
//    webView: WKWebView,
//    authenticationChallenge: NSURLAuthenticationChallenge,
//    shouldAllowDeprecatedTLS: (Boolean) -> Unit
//  ) {
//    shouldAllowDeprecatedTLS(true)
//  }

  @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
  override fun webView(
    webView: WKWebView,
    didReceiveAuthenticationChallenge: NSURLAuthenticationChallenge,
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
  ) {
    /// 这里在IO线程处理，否则会警告：This method should not be called on the main thread as it may lead to UI unresponsiveness.
    engine.remoteMM.ioAsyncScope.launch {
      if (didReceiveAuthenticationChallenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
        completionHandler(
          NSURLSessionAuthChallengeUseCredential,
          NSURLCredential.create(trust = didReceiveAuthenticationChallenge.protectionSpace.serverTrust)
        )
      } else {
        completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
      }
    }
  }
}