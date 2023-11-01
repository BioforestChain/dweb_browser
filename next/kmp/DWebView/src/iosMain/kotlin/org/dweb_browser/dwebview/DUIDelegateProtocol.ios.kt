package org.dweb_browser.dwebview

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SignalResult
import platform.WebKit.WKFrameInfo
import platform.WebKit.WKMediaCaptureType
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKPermissionDecision
import platform.WebKit.WKSecurityOrigin
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWindowFeatures
import platform.darwin.NSObject

typealias jsAlertCallback = ((WKWebView, String, WKFrameInfo) -> Unit)
typealias jsConfirmCallback = (WKWebView, String, WKFrameInfo) -> Boolean
typealias jsPromptCallbalck = (WKWebView, String, String?, WKFrameInfo) -> String



class DUIDelegateProtocol(val dwebview: DWebView) : NSObject(), WKUIDelegateProtocol {
  private val closeSignal = Signal<WKWebView>()
  private val jsAlertSignal = Signal<Pair<JsParams, SignalResult<Unit>>>()
  private val jsConfirmSignal = Signal<Pair<JsParams, SignalResult<Boolean>>>()
  private val jsPromptSignal = Signal<Pair<JsPromptParams, SignalResult<String>>>()


  internal data class JsParams(
    val webView: WKWebView,
    val message: String,
    val wkFrameInfo: WKFrameInfo
  )

  internal data class JsPromptParams(
    val webView: WKWebView,
    val message: String,
    val defaultText: String?,
    val wkFrameInfo: WKFrameInfo
  )

  override fun webView(
    webView: WKWebView,
    createWebViewWithConfiguration: WKWebViewConfiguration,
    forNavigationAction: WKNavigationAction,
    windowFeatures: WKWindowFeatures
  ): WKWebView? {

    var url = forNavigationAction.request.URL?.absoluteString


    return super.webView(
      webView,
      createWebViewWithConfiguration,
      forNavigationAction,
      windowFeatures
    )
  }

  override fun webViewDidClose(webView: WKWebView) {
    MainScope().launch {
      closeSignal.emit(webView)
    }
  }

  override fun webView(
    webView: WKWebView,
    runJavaScriptAlertPanelWithMessage: String,
    initiatedByFrame: WKFrameInfo,
    completionHandler: () -> Unit
  ) {
//    jsAlertSignal.emitForResult()
//    jsAlertSignal.emit { webview, message, frame ->
//
//
//    }


    super.webView(webView, runJavaScriptAlertPanelWithMessage, initiatedByFrame, completionHandler)
  }

  override fun webView(
    webView: WKWebView,
    runJavaScriptConfirmPanelWithMessage: String,
    initiatedByFrame: WKFrameInfo,
    completionHandler: (Boolean) -> Unit
  ) {
    super.webView(
      webView,
      runJavaScriptConfirmPanelWithMessage,
      initiatedByFrame,
      completionHandler
    )
  }

  override fun webView(
    webView: WKWebView,
    runJavaScriptTextInputPanelWithPrompt: String,
    defaultText: String?,
    initiatedByFrame: WKFrameInfo,
    completionHandler: (String?) -> Unit
  ) {
    super.webView(
      webView,
      runJavaScriptTextInputPanelWithPrompt,
      defaultText,
      initiatedByFrame,
      completionHandler
    )
  }

  override fun webView(
    webView: WKWebView,
    requestMediaCapturePermissionForOrigin: WKSecurityOrigin,
    initiatedByFrame: WKFrameInfo,
    type: WKMediaCaptureType,
    decisionHandler: (WKPermissionDecision) -> Unit
  ) {
    super.webView(
      webView,
      requestMediaCapturePermissionForOrigin,
      initiatedByFrame,
      type,
      decisionHandler
    )
  }

  private suspend fun <T, R> Signal<Pair<T, SignalResult<R>>>.emitForResult(
    args: T,
    finallyNext: Signal<Pair<T, SignalResult<R>>>
  ): Pair<R?, Boolean> {
    try {
      val list = mutableListOf<Signal<Pair<T, SignalResult<R>>>>().apply {
        add(this@emitForResult)
        add(finallyNext)
      }

      val ctx = SignalResult<R>()
      for (item in list) {
        item.emit(Pair(args, ctx))
      }

      ctx.waiter.await()
      if (ctx.hasResult) {
        return Pair(ctx.result, true)
      }
    } catch (e: Throwable) {
      debugDWebView("DUIDelegateProtocol", e.message ?: e.stackTraceToString())
    }

    return Pair(null, false)
  }
}