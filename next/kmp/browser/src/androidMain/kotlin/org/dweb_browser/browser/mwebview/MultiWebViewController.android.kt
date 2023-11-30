package org.dweb_browser.browser.mwebview

import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.atomicfu.atomic
import org.dweb_browser.dwebview.asAndroidWebView

actual fun MultiWebViewController.MultiViewItem.addWebViewClient() {
  val viewItem = this
  val webView = this.webView.asAndroidWebView()
  val minusInt = atomic(1)
  webView.addWebViewClient(object : WebViewClient() {
    override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
      if ((newScale * 100).toInt() > viewItem.currentScale) {
        val scale = viewItem.currentScale - minusInt.incrementAndGet().mod(2) - 1
        webView.setInitialScale(scale)
      } else {
        super.onScaleChanged(view, oldScale, newScale)
      }
    }
  })
}