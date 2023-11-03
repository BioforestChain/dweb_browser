package org.dweb_browser.dwebview.engine

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DNavigationDelegateProtocol
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.DWebViewWebMessage
import org.dweb_browser.dwebview.IDWebViewEngine
import org.dweb_browser.dwebview.closeWatcher.CloseWatcher
import org.dweb_browser.dwebview.closeWatcher.CloseWatcherScriptMessageHandler
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRect
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKContentWorld
import platform.WebKit.WKFrameInfo
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject


@OptIn(ExperimentalForeignApi::class)
class DWebViewEngine(
  frame: CValue<CGRect>,
  val remoteMM: MicroModule,
  val options: DWebViewOptions,
  configuration: WKWebViewConfiguration,
) : WKWebView(frame, configuration), IDWebViewEngine {
  internal val mainScope = MainScope()

  val evaluator by lazy { WebViewEvaluator(this) }

  internal val closeWatcher: CloseWatcher by lazy {
    CloseWatcher(this)
  }

  fun loadUrl(url: String) {
    val uri = Url(url)

    /// 如果是 dweb 域名，这是需要加入网关的链接前缀才能被正常加载
    if (uri.host.endsWith(".dweb") && (uri.protocol == URLProtocol.HTTP || uri.protocol == URLProtocol.HTTPS)) {
      val dwebSchemeHandler = DURLSchemeHandler(remoteMM, uri)
      configuration.setURLSchemeHandler(dwebSchemeHandler, dwebSchemeHandler.scheme)
    }

    val nsUrl = NSURL.URLWithString(url) ?: throw Exception("invalid url: ${url}")
    val navigation = super.loadRequest(NSURLRequest.requestWithURL(nsUrl))
      ?: throw Exception("fail to get WKNavigation when loadRequest")
  }

  internal val onReadySignal = SimpleSignal()

  init {
    setUIDelegate(DUIDelegateProtocol(this))
    setNavigationDelegate(object : NSObject(), WKNavigationDelegateProtocol {
      override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        mainScope.launch {
          onReadySignal.emit()
        }
      }
    })

    configuration.userContentController.apply {
      removeAllScriptMessageHandlers()
      addScriptMessageHandler(DWebViewAsyncCode(this@DWebViewEngine), "asyncCode")
      addScriptMessageHandler(CloseWatcherScriptMessageHandler(this@DWebViewEngine), "closeWatcher")
      addScriptMessageHandler(DWebViewWebMessage.WebMessagePortMessageHanlder(), "webMessagePort")
    }
    configuration.userContentController.apply {
      removeAllUserScripts()
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
          DWebViewAsyncCode.asyncCodePrepareCode,
          WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
          false
        )
      )
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
  override fun evaluateJavascriptSync(script: String) {
    evaluateJavaScript(script) { _, _ -> }
  }

  override suspend fun evaluateAsyncJavascriptCode(script: String, afterEval: suspend () -> Unit) =
    withMainContext {
      val deferred = evalAsyncJavascript(script)
      afterEval()
      deferred.await()
    }

  //#endregion

  private val navigationDelegateProtocols = mutableListOf<DNavigationDelegateProtocol>()
  fun addNavigationDelegate(protocol: DNavigationDelegateProtocol) {
    navigationDelegateProtocols.add(protocol)
  }

  fun removeNavigationDelegate(protocol: DNavigationDelegateProtocol) {
    navigationDelegateProtocols.remove(protocol)
  }
}