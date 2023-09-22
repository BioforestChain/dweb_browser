package org.dweb_browser.dwebview

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.microservice.core.MicroModule
import platform.CoreGraphics.CGRect
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

@OptIn(ExperimentalForeignApi::class)
class DWebView(
    frame: CValue<CGRect>,
    remoteMM: MicroModule,
    options: DWebViewOptions,
    configuration: WKWebViewConfiguration,
) : WKWebView(frame, configuration), IDWebView {
    override fun loadUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            super.loadRequest(NSURLRequest.requestWithURL(nsUrl))
        }
    }

    override fun destroy() {
        loadUrl("about:blank")
        removeFromSuperview()
    }

    override fun createMessageChannel(): IMessageChannel {
        TODO("Not yet implemented")
    }

    override fun setViewScale(scale: Float) {
        super.setContentScaleFactor(scale.toDouble())
    }

    override suspend fun evaluateAsyncJavascriptCode(code: String): String {
        TODO("Not yet implemented")
    }
}

