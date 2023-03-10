package info.bagen.rust.plaoc.microService.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.view.KeyEvent
import android.webkit.*
import info.bagen.rust.plaoc.microService.helper.SimpleCallback
import info.bagen.rust.plaoc.microService.helper.SimpleSignal
import kotlinx.coroutines.runBlocking

class DWebViewClient : WebViewClient() {
    private val extends = Extends<WebViewClient>()
    fun addWebViewClient(client: WebViewClient, config: Extends.Config = Extends.Config()) =
        extends.add(client, config)

    fun removeWebViewClient(client: WebViewClient) =
        extends.remove(client)

    private fun inners(methodName: String) = extends.hasMethod(methodName)
        .also { debugDWebView("WebViewClient", "calling method: $methodName") }


    class ReadyHelper : WebViewClient() {
        private val readySignal = SimpleSignal()
        fun afterReady(cb: SimpleCallback) = readySignal.listen(cb)
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            runBlocking {
                readySignal.emit()
            }
        }
    }


    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        inners("doUpdateVisitedHistory").some { it.doUpdateVisitedHistory(view, url, isReload) }
            ?: super.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
        inners("onFormResubmission").some { it.onFormResubmission(view, dontResend, resend) }
            ?: super.onFormResubmission(
                view, dontResend, resend
            )
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        inners("onLoadResource").some { it.onLoadResource(view, url) } ?: super.onLoadResource(
            view, url
        )
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        inners("onPageCommitVisible").every { it.onPageCommitVisible(view, url) }
            ?: super.onPageCommitVisible(view, url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        inners("onPageFinished").every { it.onPageFinished(view, url) } ?: super.onPageFinished(
            view, url
        )
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        inners("onPageStarted").every { it.onPageStarted(view, url, favicon) }
            ?: super.onPageStarted(
                view, url, favicon
            )
    }

    override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
        inners("onReceivedClientCertRequest").some { it.onReceivedClientCertRequest(view, request) }
            ?: super.onReceivedClientCertRequest(view, request)
    }

    override fun onReceivedError(
        view: WebView?, request: WebResourceRequest?, error: WebResourceError?
    ) {
        inners("onReceivedError").some { it.onReceivedError(view, request, error) }
            ?: super.onReceivedError(
                view, request, error
            )
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?
    ) {
        inners("onReceivedHttpAuthRequest").some {
            it.onReceivedHttpAuthRequest(
                view, handler, host, realm
            )
        } ?: super.onReceivedHttpAuthRequest(view, handler, host, realm)
    }

    override fun onReceivedHttpError(
        view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?
    ) {
        inners("onReceivedHttpError").some { it.onReceivedHttpError(view, request, errorResponse) }
            ?: super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedLoginRequest(
        view: WebView?, realm: String?, account: String?, args: String?
    ) {
        inners("onReceivedLoginRequest").some {
            it.onReceivedLoginRequest(
                view, realm, account, args
            )
        } ?: super.onReceivedLoginRequest(view, realm, account, args)
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        inners("onReceivedSslError").some { it.onReceivedSslError(view, handler, error) }
            ?: super.onReceivedSslError(view, handler, error)
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        return inners("onRenderProcessGone").lets { it.onRenderProcessGone(view, detail) }
            ?: super.onRenderProcessGone(
                view, detail
            )
    }

    override fun onSafeBrowsingHit(
        view: WebView?,
        request: WebResourceRequest?,
        threatType: Int,
        callback: SafeBrowsingResponse?
    ) {
        inners("onSafeBrowsingHit").some {
            it.onSafeBrowsingHit(
                view, request, threatType, callback
            )
        } ?: super.onSafeBrowsingHit(view, request, threatType, callback)
    }

    override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
        inners("onScaleChanged").some { it.onScaleChanged(view, oldScale, newScale) }
            ?: super.onScaleChanged(
                view, oldScale, newScale
            )
    }

    override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
        inners("onUnhandledKeyEvent").some { it.onUnhandledKeyEvent(view, event) }
            ?: super.onUnhandledKeyEvent(
                view, event
            )
    }

    override fun shouldInterceptRequest(
        view: WebView?, request: WebResourceRequest?
    ): WebResourceResponse? {
        return inners("shouldInterceptRequest").lets { it.shouldInterceptRequest(view, request) }
            ?: super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean {
        return inners("shouldOverrideKeyEvent").lets { it.shouldOverrideKeyEvent(view, event) }
            ?: super.shouldOverrideKeyEvent(view, event)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return inners("shouldOverrideUrlLoading").lets {
            it.shouldOverrideUrlLoading(
                view, request
            )
        } ?: super.shouldOverrideUrlLoading(view, request)
    }

    override fun onReceivedError(
        view: WebView?, errorCode: Int, description: String?, failingUrl: String?
    ) {
        inners("onReceivedError").some {
            it.onReceivedError(
                view, errorCode, description, failingUrl
            )
        } ?: super.onReceivedError(view, errorCode, description, failingUrl)
    }

    override fun onTooManyRedirects(view: WebView?, cancelMsg: Message?, continueMsg: Message?) {
        inners("onTooManyRedirects").some { it.onTooManyRedirects(view, cancelMsg, continueMsg) }
            ?: super.onTooManyRedirects(view, cancelMsg, continueMsg)
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        return inners("shouldInterceptRequest").lets { it.shouldInterceptRequest(view, url) }
            ?: super.shouldInterceptRequest(
                view, url
            )
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return inners("shouldOverrideUrlLoading").lets { it.shouldOverrideUrlLoading(view, url) }
            ?: super.shouldOverrideUrlLoading(view, url)
    }
}