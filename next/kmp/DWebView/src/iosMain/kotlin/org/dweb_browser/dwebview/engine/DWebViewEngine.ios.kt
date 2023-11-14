package org.dweb_browser.dwebview.engine

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.DWebViewWebMessage
import org.dweb_browser.dwebview.WebBeforeUnloadArgs
import org.dweb_browser.dwebview.WebLoadErrorState
import org.dweb_browser.dwebview.WebLoadStartState
import org.dweb_browser.dwebview.WebLoadState
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.dwebview.closeWatcher.CloseWatcher
import org.dweb_browser.dwebview.closeWatcher.CloseWatcherScriptMessageHandler
import org.dweb_browser.dwebview.toReadyListener
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRect
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIDevice
import platform.WebKit.WKContentWorld
import platform.WebKit.WKFrameInfo
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKPreferences
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.javaScriptEnabled
import platform.darwin.NSObject


@OptIn(ExperimentalForeignApi::class)
class DWebViewEngine(
  frame: CValue<CGRect>,
  val remoteMM: MicroModule,
  internal val options: DWebViewOptions,
  configuration: WKWebViewConfiguration,
) : WKWebView(frame, configuration.also {
  if (options.url.isNotEmpty()) {
    tryRegistryUrlSchemeHandler(options.url, remoteMM, it)
  }
}) {
  internal val mainScope = MainScope()

  val evaluator by lazy { WebViewEvaluator(this) }

  val loadStateChangeSignal = Signal<WebLoadState>()
  val onReady by lazy { loadStateChangeSignal.toReadyListener() }
  val beforeUnloadSignal = Signal<WebBeforeUnloadArgs>()

  init {
    @Suppress("CONFLICTING_OVERLOADS")
    setNavigationDelegate(object : NSObject(), WKNavigationDelegateProtocol {
      override fun webView(
        webView: WKWebView,
        navigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit
      ) {
        /// navigationAction.navigationType : https://developer.apple.com/documentation/webkit/wknavigationtype/
        val message = when (navigationAction.navigationType) {
          // reload
          3L -> "重新加载此网站？"
          else -> "离开此网站？"
        }
        val confirm = runBlocking {
          val args = WebBeforeUnloadArgs(message)
          beforeUnloadSignal.emit(args)
          args.waitHookResults()
        }
        if (confirm) {
          decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        } else {
          decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        }
      }

      override fun webView(
        webView: WKWebView,
        didStartProvisionalNavigation: WKNavigation?
      ) {
        val loadedUrl = webView.URL?.absoluteString ?: "about:blank"
        mainScope.launch { loadStateChangeSignal.emit(WebLoadStartState(loadedUrl)) }
      }

      override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        val loadedUrl = webView.URL?.absoluteString ?: "about:blank"
        mainScope.launch { loadStateChangeSignal.emit(WebLoadSuccessState(loadedUrl)) }
      }

      override fun webView(
        webView: WKWebView,
        didFailNavigation: WKNavigation?,
        withError: NSError
      ) {
        val currentUrl = webView.URL?.absoluteString ?: "about:blank"
        val errorMessage = "[${withError.code}]$currentUrl\n${withError.description}"
        mainScope.launch { loadStateChangeSignal.emit(WebLoadErrorState(currentUrl, errorMessage)) }
      }
    })
  }

  internal val closeWatcher: CloseWatcher by lazy {
    CloseWatcher(this)
  }

  companion object {
    @Deprecated("use proxy for https://*.dweb")
    private fun tryRegistryUrlSchemeHandler(
      url: String,
      remoteMM: MicroModule,
      configuration: WKWebViewConfiguration
    ) {
      val baseUri = Url(url)
      /// 如果是 dweb 域名，这是需要加入网关的链接前缀才能被正常加载
      if (baseUri.host.endsWith(".dweb") && (baseUri.protocol == URLProtocol.HTTP || baseUri.protocol == URLProtocol.HTTPS)) {
        val dwebSchemeHandler = DURLSchemeHandler(remoteMM, baseUri)
        configuration.setURLSchemeHandler(dwebSchemeHandler, dwebSchemeHandler.scheme)
      }
    }
  }

  fun loadUrl(url: String) {
    var uri = Url(url)

    if (uri.host.endsWith(".dweb") && (uri.protocol == URLProtocol.HTTP || uri.protocol == URLProtocol.HTTPS)) {
      uri = Url("${DURLSchemeHandler.getScheme(uri)}:${uri.encodedPathAndQuery}")
    }
    val nsUrl = NSURL.URLWithString(uri.toString()) ?: throw Exception("invalid url: $url")
    val navigation = loadRequest(NSURLRequest.requestWithURL(nsUrl))
      ?: throw Exception("fail to get WKNavigation when loadRequest")
  }

  init {
    /// 测试的时候使用
    if (UIDevice.currentDevice.systemVersion.compareTo("16.4", true) >= 0) {
      this.setInspectable(true)
    }
    setUIDelegate(DUIDelegateProtocol(this))

    val preferences = WKPreferences()
    preferences.javaScriptEnabled = true
    preferences.javaScriptCanOpenWindowsAutomatically = false
    configuration.preferences = preferences
    configuration.userContentController.apply {
      removeAllScriptMessageHandlers()
      removeAllUserScripts()
      addScriptMessageHandler(
        LogScriptMessageHandler(),
        DWebViewWebMessage.webMessagePortContentWorld,
        "log"
      )
      val dWebViewAsyncCode = DWebViewAsyncCode(this@DWebViewEngine)
      addScriptMessageHandler(dWebViewAsyncCode, "asyncCode")
      addScriptMessageHandler(CloseWatcherScriptMessageHandler(this@DWebViewEngine), "closeWatcher")
      addScriptMessageHandler(
        DWebViewWebMessage.WebMessagePortMessageHanlder(),
        DWebViewWebMessage.webMessagePortContentWorld,
        "webMessagePort"
      )
      addUserScript(
        WKUserScript(
          DWebViewWebMessage.WebMessagePortPrepareCode,
          WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
          false,
          DWebViewWebMessage.webMessagePortContentWorld
        )
      )
      addUserScript(
        WKUserScript(
          dWebViewAsyncCode.asyncCodePrepareCode,
          WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
          false,
        )
      )
    }

    if (options.url.isNotEmpty()) {
      loadUrl(options.url)
    }
  }

  fun evalAsyncJavascript(code: String): Deferred<String> {
    val deferred = CompletableDeferred<String>()
    evaluateJavaScript(code) { result, error ->
      if (error == null) {
        deferred.complete(result as String)
      } else {
        deferred.completeExceptionally(Throwable(error.localizedDescription))
      }
    }

    return deferred
  }

  fun <T> evalAsyncJavascript(
    code: String, wkFrameInfo: WKFrameInfo?, wkContentWorld: WKContentWorld
  ): Deferred<T> {
    val deferred = CompletableDeferred<T>()
    evaluateJavaScript(code, wkFrameInfo, wkContentWorld) { result, error ->
      if (error == null) {
        deferred.complete(result as T)
      } else {
        deferred.completeExceptionally(Throwable(error.localizedDescription))
      }
    }

    return deferred
  }

  fun <T> callAsyncJavaScript(
    functionBody: String,
    arguments: Map<Any?, *>?,
    inFrame: WKFrameInfo?,
    inContentWorld: WKContentWorld,
  ): Deferred<T> {
    val deferred = CompletableDeferred<T>()

    callAsyncJavaScript(functionBody, arguments, inFrame, inContentWorld) { result, error ->
      if (error == null) {
        deferred.complete(result as T)
      } else {
        deferred.completeExceptionally(Throwable(error.localizedDescription))
      }
    }

    return deferred
  }

  //#region 用于 CloseWatcher
  fun evaluateJavascriptSync(script: String) {
    evaluateJavaScript(script) { _, _ -> }
  }

  suspend fun evaluateAsyncJavascriptCode(script: String, afterEval: suspend () -> Unit = {}) =
    withMainContext {
      val deferred = evalAsyncJavascript(script)
      afterEval()
      deferred.await()
    }

  //#endregion
}