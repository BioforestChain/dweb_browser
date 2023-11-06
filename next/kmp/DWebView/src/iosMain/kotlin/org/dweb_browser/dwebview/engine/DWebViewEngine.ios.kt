package org.dweb_browser.dwebview.engine

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DWebMessageChannel
import org.dweb_browser.dwebview.DWebMessagePort
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.DWebViewWebMessage
import org.dweb_browser.dwebview.IMessageChannel
import org.dweb_browser.dwebview.closeWatcher.CloseWatcher
import org.dweb_browser.dwebview.closeWatcher.CloseWatcherScriptMessageHandler
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRect
import platform.Foundation.NSArray
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKContentWorld
import platform.WebKit.WKFrameInfo
import platform.WebKit.WKPreferences
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.javaScriptEnabled


@OptIn(ExperimentalForeignApi::class)
class DWebViewEngine(
  frame: CValue<CGRect>,
  val remoteMM: MicroModule,
  private val options: DWebViewOptions,
  configuration: WKWebViewConfiguration,
) : WKWebView(frame, configuration.also {
  if (options.url.isNotEmpty()) {
    tryRegistryUrlSchemeHandler(options.url, remoteMM, it)
  }
}) {
  internal val mainScope = MainScope()

  val evaluator by lazy { WebViewEvaluator(this) }

  internal val closeWatcher: CloseWatcher by lazy {
    CloseWatcher(this)
  }

  companion object {
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

  val onReadySignal = SimpleSignal()

  init {
    /// 测试的时候使用
    this.setInspectable(true)
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