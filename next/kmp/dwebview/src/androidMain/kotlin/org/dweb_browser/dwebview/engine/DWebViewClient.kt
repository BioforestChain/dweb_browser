package org.dweb_browser.dwebview.engine

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.KeyEvent
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ktor.http.Url
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.mapFindNoNull
import org.dweb_browser.helper.one
import org.dweb_browser.helper.someOrNull

class DWebViewClient(val engine: DWebViewEngine) : WebViewClient() {
  private val scope get() = engine.lifecycleScope
  private val extends = Extends<WebViewClient>()
  fun addWebViewClient(client: WebViewClient, config: Extends.Config = Extends.Config()) =
    extends.add(client, config)

  fun removeWebViewClient(client: WebViewClient) =
    extends.remove(client)

  private fun inners(methodName: String) = extends.hasMethod(methodName)
    .also { debugDWebView("WebViewClient", "calling method: $methodName") }


  override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
    inners("doUpdateVisitedHistory").forEach { it.doUpdateVisitedHistory(view, url, isReload) }
    super.doUpdateVisitedHistory(view, url, isReload)
  }

  override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
    inners("onFormResubmission").one { it.onFormResubmission(view, dontResend, resend) }
      ?: super.onFormResubmission(
        view, dontResend, resend
      )
  }

  override fun onLoadResource(view: WebView?, url: String?) {
    inners("onLoadResource").one { it.onLoadResource(view, url) } ?: super.onLoadResource(
      view, url
    )
  }

  override fun onPageCommitVisible(view: WebView, url: String) {
    inners("onPageCommitVisible").forEach { it.onPageCommitVisible(view, url) };
    super.onPageCommitVisible(view, url)
  }


  override fun onPageFinished(view: WebView, url: String?) {
    inners("onPageFinished").forEach { it.onPageFinished(view, url) };
    super.onPageFinished(view, url)
  }

  override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
    inners("onPageStarted").forEach { it.onPageStarted(view, url, favicon) };
    super.onPageStarted(view, url, favicon)
  }

  override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
    inners("onReceivedClientCertRequest").one { it.onReceivedClientCertRequest(view, request) }
      ?: super.onReceivedClientCertRequest(view, request)
  }

  override fun onReceivedError(
    view: WebView, request: WebResourceRequest?, error: WebResourceError?,
  ) {
    inners("onReceivedError").forEach { it.onReceivedError(view, request, error) }
    super.onReceivedError(view, request, error)
  }

  override fun onReceivedHttpAuthRequest(
    view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?,
  ) {
    inners("onReceivedHttpAuthRequest").one {
      it.onReceivedHttpAuthRequest(
        view, handler, host, realm
      )
    } ?: super.onReceivedHttpAuthRequest(view, handler, host, realm)
  }

  override fun onReceivedHttpError(
    view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?,
  ) {
    debugDWebView("onReceivedHttpError", "${request?.url} error:${errorResponse}")
    inners("onReceivedHttpError").one { it.onReceivedHttpError(view, request, errorResponse) }
      ?: super.onReceivedHttpError(view, request, errorResponse)
  }

  override fun onReceivedLoginRequest(
    view: WebView?, realm: String?, account: String?, args: String?,
  ) {
    inners("onReceivedLoginRequest").one {
      it.onReceivedLoginRequest(
        view, realm, account, args
      )
    } ?: super.onReceivedLoginRequest(view, realm, account, args)
  }

  @SuppressLint("WebViewClientOnReceivedSslError")
  override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
    if (view != null && view.url != null) {
      val url = Url(view.url!!)

      if (url.host.endsWith(".dweb")) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          println("webviewClient onReceivedSslError $error ${error?.certificate?.x509Certificate?.publicKey?.encoded}")
        }
        handler?.proceed()
        return
      }
    }
    inners("onReceivedSslError").one { it.onReceivedSslError(view, handler, error) }
      ?: super.onReceivedSslError(view, handler, error)
  }

  override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
    return inners("onRenderProcessGone").mapFindNoNull { it.onRenderProcessGone(view, detail) }
      ?: super.onRenderProcessGone(
        view, detail
      )
  }

  override fun onSafeBrowsingHit(
    view: WebView?,
    request: WebResourceRequest?,
    threatType: Int,
    callback: SafeBrowsingResponse?,
  ) {
    inners("onSafeBrowsingHit").one {
      it.onSafeBrowsingHit(
        view, request, threatType, callback
      )
    } ?: super.onSafeBrowsingHit(view, request, threatType, callback)
  }

  override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
    inners("onScaleChanged").one { it.onScaleChanged(view, oldScale, newScale) }
      ?: super.onScaleChanged(
        view, oldScale, newScale
      )
  }

  override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
    inners("onUnhandledKeyEvent").one { it.onUnhandledKeyEvent(view, event) }
      ?: super.onUnhandledKeyEvent(
        view, event
      )
  }

  override fun shouldInterceptRequest(
    view: WebView?, request: WebResourceRequest?,
  ): WebResourceResponse? {
    return inners("shouldInterceptRequest").mapFindNoNull {
      it.shouldInterceptRequest(
        view,
        request
      )
    }
      ?: super.shouldInterceptRequest(view, request)
  }

  override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean {
    return inners("shouldOverrideKeyEvent").someOrNull { it.shouldOverrideKeyEvent(view, event) }
      ?: super.shouldOverrideKeyEvent(view, event)
  }

  override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
    return inners("shouldOverrideUrlLoading").someOrNull {
      it.shouldOverrideUrlLoading(view, request)
    } ?: super.shouldOverrideUrlLoading(view, request)
  }

  @Deprecated("Deprecated in Java")
  override fun onReceivedError(
    view: WebView?, errorCode: Int, description: String?, failingUrl: String?,
  ) {
    inners("onReceivedError").one {
      it.onReceivedError(
        view, errorCode, description, failingUrl
      )
    } ?: super.onReceivedError(view, errorCode, description, failingUrl)
  }

  @Deprecated("Deprecated in Java")
  override fun onTooManyRedirects(view: WebView?, cancelMsg: Message?, continueMsg: Message?) {
    inners("onTooManyRedirects").one { it.onTooManyRedirects(view, cancelMsg, continueMsg) }
      ?: super.onTooManyRedirects(view, cancelMsg, continueMsg)
  }

  @Deprecated("Deprecated in Java")
  override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
    return inners("shouldInterceptRequest").mapFindNoNull { it.shouldInterceptRequest(view, url) }
      ?: super.shouldInterceptRequest(view, url)
  }

  @Deprecated("Deprecated in Java")
  override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
    return inners("shouldOverrideUrlLoading").someOrNull {
      it.shouldOverrideUrlLoading(view, url)
    } ?: super.shouldOverrideUrlLoading(view, url)
  }
}