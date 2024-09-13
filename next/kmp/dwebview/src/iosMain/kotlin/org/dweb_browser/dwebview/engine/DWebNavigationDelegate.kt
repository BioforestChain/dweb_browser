package org.dweb_browser.dwebview.engine

import androidx.compose.ui.util.fastJoinToString
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.UrlLoadingPolicy
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.dwebview.debugDWebView
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

  class DecidePolicyForNavigationActionContext(
    val webView: WKWebView,
    val decidePolicyForNavigationAction: WKNavigationAction,
    val preferences: WKWebpagePreferences?,
  ) {
    val loadedUrl by lazy {
      decidePolicyForNavigationAction.request.URL?.absoluteString
    }
  }

  val decidePolicyForNavigationActionHooks =
    mutableListOf<suspend DecidePolicyForNavigationActionContext.() -> UrlLoadingPolicy>()

  private fun decidePolicyForNavigationAction(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    preferences: WKWebpagePreferences?,
    decisionHandler: (WKNavigationActionPolicy) -> Unit,
  ) {
    debugDWebView("Nav/decidePolicyForNavigationAction") { "loadedUrl=${decidePolicyForNavigationAction.request.URL?.absoluteString}" }
    DecidePolicyForNavigationActionContext(
      webView,
      decidePolicyForNavigationAction,
      preferences
    ).apply {
      engine.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
        var allAllow = true
        decidePolicyForNavigationActionHooks.map { hook ->
          launch {
            if (UrlLoadingPolicy.Block == hook()) {
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
  }

  private var needDownloadUrlSet = mutableSetOf<String>()

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    preferences: WKWebpagePreferences,
    decisionHandler: (WKNavigationActionPolicy, WKWebpagePreferences?) -> Unit,
  ) {
    if (decidePolicyForNavigationAction.shouldPerformDownload) {/*
      * 为了拿到download需要的MIME, suggestedFilename, content-lenght等数据，
      * 先妥协将download标签标识的href链接的处理放置在decidePolicyForNavigationResponse阶段。
      * 这边先做保存需要download的url。
      * */
      decidePolicyForNavigationAction.request.URL?.absoluteString?.let {
        needDownloadUrlSet.add(it)
      }
    }

    decidePolicyForNavigationAction(webView, decidePolicyForNavigationAction, preferences) {
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
    decidePolicyForNavigationAction(webView, decidePolicyForNavigationAction, null, decisionHandler)
  }

  class DidStartProvisionalNavigationContext(
    val webView: WKWebView, val didStartProvisionalNavigation: WKNavigation?,
  ) {
    val loadedUrl by lazy { webView.URL?.absoluteString ?: "about:blank" }
  }

  val didStartProvisionalNavigationHooks =
    mutableListOf<suspend DidStartProvisionalNavigationContext.() -> Unit>()

  @ObjCSignatureOverride
  override fun webView(
    webView: WKWebView, didStartProvisionalNavigation: WKNavigation?,
  ) {
    DidStartProvisionalNavigationContext(webView, didStartProvisionalNavigation).apply {
      debugDWebView("Nav/didStartProvisionalNavigation") { "loadedUrl=$loadedUrl websiteDataStore=${engine.configuration.websiteDataStore} proxyConfigurations=${engine.configuration.websiteDataStore.proxyConfigurations?.fastJoinToString()}" }
      engine.mainScope.launch {
        for (hook in didStartProvisionalNavigationHooks) {
          launch { hook() }
        }
      }
    }
  }

  class DidFinishNavigation(
    val webView: WKWebView, val didFinishNavigation: WKNavigation?,
  ) {
    val loadedUrl by lazy { webView.URL?.absoluteString ?: "about:blank" }
  }

  val didFinishNavigationHooks = mutableListOf<suspend DidFinishNavigation.() -> Unit>()

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
    DidFinishNavigation(webView, didFinishNavigation).apply {
      debugDWebView("Nav/didFinishNavigation") { "loadedUrl=$loadedUrl" }
      engine.mainScope.launch {
        for (hook in didFinishNavigationHooks) {
          launch { hook() }
        }
      }
    }
  }

  class DidFailNavigation(
    val webView: WKWebView,
    val didFailNavigation: WKNavigation?,
    val withError: NSError,
  ) {
    val currentUrl by lazy { webView.URL?.absoluteString ?: "about:blank" }
  }

  val didFailNavigationHooks = mutableListOf<suspend DidFailNavigation.() -> Unit>()

  override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
    DidFailNavigation(webView, didFailNavigation, withError).apply {
      debugDWebView("Nav/didFailNavigation") { "currentUrl=$currentUrl" }
      engine.mainScope.launch {
        for (hook in didFailNavigationHooks) {
          launch { hook() }
        }
      }
    }
  }

//  override fun webView(
//    webView: WKWebView,
//    authenticationChallenge: NSURLAuthenticationChallenge,
//    shouldAllowDeprecatedTLS: (Boolean) -> Unit
//  ) {
//    shouldAllowDeprecatedTLS(true)
//  }

  @OptIn(ExperimentalForeignApi::class)
  override fun webView(
    webView: WKWebView,
    didReceiveAuthenticationChallenge: NSURLAuthenticationChallenge,
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
  ) {
    debugDWebView("Nav/didReceiveAuthenticationChallenge") {
      "authenticationMethod=${didReceiveAuthenticationChallenge.protectionSpace.authenticationMethod}/${NSURLAuthenticationMethodServerTrust}"
    }
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