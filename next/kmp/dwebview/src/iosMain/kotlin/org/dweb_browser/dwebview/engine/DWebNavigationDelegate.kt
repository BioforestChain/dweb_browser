package org.dweb_browser.dwebview.engine

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.UrlLoadingPolicy
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.dwebview.WebLoadErrorState
import org.dweb_browser.dwebview.WebLoadStartState
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.helper.globalMainScope
import platform.Foundation.NSError
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLResponse
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

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
class DWebNavigationDelegate(internal val engine: DWebViewEngine) : NSObject(),
  WKNavigationDelegateProtocol {
  override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
    engine.mainScope.launch {
      engine.closeSignal.emit()
    }
  }

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationResponse: WKNavigationResponse,
    decisionHandler: (WKNavigationResponsePolicy) -> Unit,
  ) {
    val url = decidePolicyForNavigationResponse.response.URL?.absoluteString ?: ""
    //被标识位download的url, 或者无法被原生webview处理的MIME格式。执行download流程。
    if (needDownloadUrlSet.contains(url) || !decidePolicyForNavigationResponse.canShowMIMEType()) {
      needDownloadUrlSet.remove(url)
      decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyCancel)
      doDownload(decidePolicyForNavigationResponse.response)
    } else {
      decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)
    }
  }

  val decidePolicyForNavigationActionHooks = mutableListOf<suspend (
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
  ) -> UrlLoadingPolicy>()

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun decidePolicyForNavigationAction(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    decisionHandler: (WKNavigationActionPolicy) -> Unit,
  ) {
    engine.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
      var allAllow = true
      decidePolicyForNavigationActionHooks.map { hook ->
        launch {
          if (UrlLoadingPolicy.Block == hook(webView, decidePolicyForNavigationAction)) {
            allAllow = false
          }
        }
      }.joinAll()
      decisionHandler(
        when {
          allAllow -> WKNavigationActionPolicy.WKNavigationActionPolicyAllow
          else -> WKNavigationActionPolicy.WKNavigationActionPolicyCancel
        }
      )
    }
  }

  private var needDownloadUrlSet = mutableSetOf<String>()

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    preferences: WKWebpagePreferences,
    decisionHandler: (WKNavigationActionPolicy, WKWebpagePreferences?) -> Unit,
  ) {
    if (decidePolicyForNavigationAction.shouldPerformDownload) {
      /*
      * 为了拿到download需要的MIME, suggestedFilename, content-lenght等数据，
      * 先妥协将download标签标识的href链接的处理放置在decidePolicyForNavigationResponse阶段。
      * 这边先做保存需要download的url。
      * */
      decidePolicyForNavigationAction.request.URL?.absoluteString?.let {
        needDownloadUrlSet.add(it)
      }
    }

    decidePolicyForNavigationAction(webView, decidePolicyForNavigationAction) {
      decisionHandler(it, null)
    }
  }

  private fun doDownload(response: NSURLResponse) {
    globalMainScope.launch {
      val arg = WebDownloadArgs(
        engine.customUserAgent() ?: "",
        response.suggestedFilename ?: "",
        response.MIMEType ?: "",
        response.expectedContentLength(),
        response.URL?.absoluteString() ?: ""
      )
      engine.downloadSignal.emit(arg)
    }
  }

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    decisionHandler: (WKNavigationActionPolicy) -> Unit,
  ) {
    decidePolicyForNavigationAction(webView, decidePolicyForNavigationAction, decisionHandler)
  }

  @ObjCSignatureOverride
  override fun webView(
    webView: WKWebView, didStartProvisionalNavigation: WKNavigation?,
  ) {
    val loadedUrl = webView.URL?.absoluteString ?: "about:blank"
    engine.mainScope.launch { engine.loadStateChangeSignal.emit(WebLoadStartState(loadedUrl)) }
  }

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
    val loadedUrl = webView.URL?.absoluteString ?: "about:blank"
    engine.mainScope.launch {
      engine.loadStateChangeSignal.emit(WebLoadSuccessState(loadedUrl))
    }
  }

  override fun webView(
    webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError,
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
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
  ) {
    /// 这里在IO线程处理，否则会警告：This method should not be called on the main thread as it may lead to UI unresponsiveness.
    engine.lifecycleScope.launch {
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