package org.dweb_browser.dwebview.engine

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.hostWithPort
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.DWebViewWebMessage
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.WebBeforeUnloadArgs
import org.dweb_browser.dwebview.WebLoadErrorState
import org.dweb_browser.dwebview.WebLoadStartState
import org.dweb_browser.dwebview.WebLoadState
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.dwebview.closeWatcher.CloseWatcher
import org.dweb_browser.dwebview.closeWatcher.CloseWatcherScriptMessageHandler
import org.dweb_browser.dwebview.toReadyListener
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.compose.transparentColor
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.ios.DwebHelper
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRect
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.create
import platform.Foundation.serverTrust
import platform.UIKit.UIColor
import platform.UIKit.UIDevice
import platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior
import platform.WebKit.WKContentWorld
import platform.WebKit.WKFrameInfo
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKPreferences
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebpagePreferences
import platform.WebKit.javaScriptEnabled


@Suppress("CONFLICTING_OVERLOADS")
@OptIn(ExperimentalForeignApi::class)
class DWebViewEngine(
  frame: CValue<CGRect>,
  val remoteMM: MicroModule,
  internal val options: DWebViewOptions,
  configuration: WKWebViewConfiguration,
) : WKWebView(frame, configuration.also {
  registryDwebHttpUrlSchemeHandler(remoteMM, it)
  registryDwebSchemeHandler(remoteMM, it)
  remoteMM.ioAsyncScope.launch {
    val url = Url(IDWebView.getProxyAddress())
    withMainContext {
      @Suppress("USELESS_CAST")
      DwebHelper().setProxyWithConfiguration(
        // 强制类型转换，不然WKWebViewConfiguration会提示类型对不上
        it as objcnames.classes.WKWebViewConfiguration, url.host, url.port.toUShort()
      )
    }
  }
}), WKNavigationDelegateProtocol {
  internal val mainScope = CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
  internal val ioScope = CoroutineScope(remoteMM.ioAsyncScope.coroutineContext + SupervisorJob())

  val loadStateChangeSignal = Signal<WebLoadState>()
  val onReady by lazy { loadStateChangeSignal.toReadyListener() }
  val beforeUnloadSignal = Signal<WebBeforeUnloadArgs>()
  val loadingProgressSharedFlow = MutableSharedFlow<Float>()
  val closeSignal = SimpleSignal()
  val createWindowSignal = Signal<IDWebView>()

  internal val closeWatcher: CloseWatcher by lazy {
    CloseWatcher(this)
  }

  companion object {
    /**
     * 注册 dweb+http(s)? 的链接拦截，因为IOS不能拦截 `http(s)?:*.dweb`。
     * 所以这里定义了这个特殊的 scheme 来替代 http(s)?:*.dweb
     *
     * PS：IOS17+可以拦截 https:*.dweb。但是这需要依赖与网络技术栈
     * 所以 http(s)?:*.dweb 在 IOS上 反而是一个更加安全的、仅走内存控制的技术，通常用于内部模块使用
     */
    private fun registryDwebHttpUrlSchemeHandler(
      remoteMM: MicroModule, configuration: WKWebViewConfiguration
    ) {
      val dwebSchemeHandler = DwebHttpURLSchemeHandler(remoteMM)
      configuration.setURLSchemeHandler(dwebSchemeHandler, "dweb+http")
      configuration.setURLSchemeHandler(dwebSchemeHandler, "dweb+https")
    }

    fun registryDwebSchemeHandler(remoteMM: MicroModule, configuration: WKWebViewConfiguration) {
      val dwebSchemeHandler = DwebURLSchemeHandler(remoteMM)
      configuration.setURLSchemeHandler(dwebSchemeHandler, "dweb")
    }
  }

  suspend fun loadUrl(url: String): String {
    val safeUrl = resolveUrl(url)
    val nsUrl = NSURL(string = safeUrl)
    val nav = loadRequest(NSURLRequest(uRL = nsUrl))
    println("QAQ loadUrl end: $nsUrl => $nav")
    return safeUrl
  }

  suspend fun resolveUrl(inputUrl: String): String {
    val safeUrl = if (inputUrl.contains(".dweb")) {
      Url(inputUrl).let { url ->
        /// 处理 http(s)?:*.dweb
        if (url.host.endsWith(".dweb")) {
          if (url.protocol == URLProtocol.HTTP || (options.privateNet && url.protocol == URLProtocol.HTTPS)) {
            "dweb+$url"
          } else inputUrl
        } else {
          /// 处理 http://*.dweb-port.localhost:gateway-port
          val httpLocalhostGatewaySuffix = dwebHttpGatewayServer.getHttpLocalhostGatewaySuffix()
          val inputHostWithPort = url.hostWithPort
          if (url.protocol == URLProtocol.HTTP && inputHostWithPort.endsWith(
              httpLocalhostGatewaySuffix
            )
          ) {
            "dweb+" + inputUrl.replace(inputHostWithPort, inputHostWithPort.substring(
              0, inputHostWithPort.length - httpLocalhostGatewaySuffix.length
            ).let { dwebHost ->
                val hostInfo = dwebHost.split('-')
                val port = hostInfo.last().toUShortOrNull()
                if (port != null) {
                  hostInfo.toMutableList().run {
                    removeLast()
                    joinToString("-")
                  } + ":$port"
                } else dwebHost
              })
          } else inputUrl
        }
      }
    } else inputUrl
    return safeUrl
  }

  init {
    /// 测试的时候使用
    if (UIDevice.currentDevice.systemVersion.compareTo("16.4", true) >= 0) {
      this.setInspectable(true)
    }
    setNavigationDelegate(this)
    setUIDelegate(DUIDelegateProtocol(this))

    val preferences = WKPreferences()
    preferences.javaScriptEnabled = true
    preferences.javaScriptCanOpenWindowsAutomatically = false
    configuration.preferences = preferences
    configuration.userContentController.apply {
      removeAllScriptMessageHandlers()
      removeAllUserScripts()
      addScriptMessageHandler(CloseWatcherScriptMessageHandler(this@DWebViewEngine), "closeWatcher")
      addScriptMessageHandler(
        DWebViewWebMessage.WebMessagePortMessageHandler(),
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
    }

    if (options.url.isNotEmpty()) {
      mainScope.launch {
        loadUrl(options.url)
      }
    }

    setOpaque(false)
    setBackgroundColor(UIColor.transparentColor)
    scrollView.setBackgroundColor(UIColor.transparentColor)
    if (options.displayCutoutStrategy == DWebViewOptions.DisplayCutoutStrategy.Ignore) {
      scrollView.contentInsetAdjustmentBehavior =
        UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
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

  suspend fun <T> callAsyncJavaScript(
    functionBody: String,
    arguments: Map<Any?, *>? = null,
    inFrame: WKFrameInfo? = null,
    inContentWorld: WKContentWorld = WKContentWorld.pageWorld,
    afterEval: (suspend () -> Unit)? = null
  ): T {
    val deferred = CompletableDeferred<T>()
    println("callAsyncJavaScript-start: $functionBody")
    callAsyncJavaScript(functionBody, arguments, inFrame, inContentWorld) { result, error ->
      println("callAsyncJavaScript-return: functionBody:$functionBody result:$result error:$error")
      if (error == null) {
        deferred.complete(result as T)
      } else {
        deferred.completeExceptionally(Throwable(error.localizedDescription))
      }
    }
    afterEval?.invoke()

    return deferred.await()
  }

  //#region 用于 CloseWatcher
  fun evaluateJavascriptSync(script: String) {
    evaluateJavaScript(script) { _, _ -> }
  }

  //#endregion

  //#region NavigationDelegate
  override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
    mainScope.launch {
      closeSignal.emit()
    }
  }

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationResponse: WKNavigationResponse,
    decisionHandler: (WKNavigationResponsePolicy) -> Unit
  ) {
    decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)
  }

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    preferences: WKWebpagePreferences,
    decisionHandler: (WKNavigationActionPolicy, WKWebpagePreferences?) -> Unit
  ) {
    decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow, preferences)
  }

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    decisionHandler: (WKNavigationActionPolicy) -> Unit
  ) {
    var confirm = true
    /// navigationAction.navigationType : https://developer.apple.com/documentation/webkit/wknavigationtype/
    if (beforeUnloadSignal.isNotEmpty()) {
      val message = when (decidePolicyForNavigationAction.navigationType) {
        // reload
        3L -> "重新加载此网站？"
        else -> "离开此网站？"
      }
      confirm = runBlocking {
        val args = WebBeforeUnloadArgs(message)
        beforeUnloadSignal.emit(args)
        args.waitHookResults()
      }
    }
    println("QAQ decidePolicyForNavigationAction: $confirm")
    if (confirm) {
//          if (decidePolicyForNavigationAction.targetFrame == null) {
//            loadRequest(decidePolicyForNavigationAction.request)
//          }
      decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
    } else {
      decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
    }
  }

  override fun webView(
    webView: WKWebView, didStartProvisionalNavigation: WKNavigation?
  ) {
    println("QAQ didStartProvisionalNavigation: $didStartProvisionalNavigation")
    val loadedUrl = webView.URL?.absoluteString ?: "about:blank"
    mainScope.launch { loadStateChangeSignal.emit(WebLoadStartState(loadedUrl)) }
  }

  override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
    println("QAQ didFinishNavigation: $didFinishNavigation")
    val loadedUrl = webView.URL?.absoluteString ?: "about:blank"
    mainScope.launch { loadStateChangeSignal.emit(WebLoadSuccessState(loadedUrl)) }
  }

  override fun webView(
    webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError
  ) {
    println("QAQ didFailNavigation: $didFailNavigation")
    val currentUrl = webView.URL?.absoluteString ?: "about:blank"
    val errorMessage = "[${withError.code}]$currentUrl\n${withError.description}"
    mainScope.launch { loadStateChangeSignal.emit(WebLoadErrorState(currentUrl, errorMessage)) }
  }

//  override fun webView(
//    webView: WKWebView,
//    authenticationChallenge: NSURLAuthenticationChallenge,
//    shouldAllowDeprecatedTLS: (Boolean) -> Unit
//  ) {
//    shouldAllowDeprecatedTLS(true)
//  }

  @OptIn(BetaInteropApi::class)
  override fun webView(
    webView: WKWebView,
    didReceiveAuthenticationChallenge: NSURLAuthenticationChallenge,
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
  ) {
    if (didReceiveAuthenticationChallenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
      completionHandler(
        NSURLSessionAuthChallengeUseCredential,
        NSURLCredential.create(trust = didReceiveAuthenticationChallenge.protectionSpace.serverTrust)
      )
      return
    }

    completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
  }
  //#endregion
}