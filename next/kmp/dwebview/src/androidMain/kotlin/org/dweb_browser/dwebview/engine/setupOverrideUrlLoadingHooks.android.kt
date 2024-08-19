package org.dweb_browser.dwebview.engine

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.dweb_browser.dwebview.OverrideUrlLoadingParams
import org.dweb_browser.dwebview.UrlLoadingPolicy

fun setupOverrideUrlLoadingHooks(
  engine: DWebViewEngine,
) = mutableListOf<OverrideUrlLoadingParams.() -> UrlLoadingPolicy>().also { hooks ->
  engine.dWebViewClient.addWebViewClient(object : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
      val params = OverrideUrlLoadingParams(
        request.url.toString(),
        request.isForMainFrame
      )
      for (hook in hooks) {
        return when (params.hook()) {
          UrlLoadingPolicy.Allow -> continue
          UrlLoadingPolicy.Block -> true
        }
      }
      return super.shouldOverrideUrlLoading(view, request)
    }
  })
}