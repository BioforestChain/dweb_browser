package org.dweb_browser.dwebview.engine

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.dwebview.WebLoadErrorState
import org.dweb_browser.dwebview.WebLoadStartState
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.dwebview.base.isWebUrlScheme

fun setupLoadStateFlow(engine: DWebViewEngine, initUrl: String) = MutableStateFlow(
  when (initUrl) {
    "", "about:blank" -> WebLoadSuccessState("about:blank")
    else -> WebLoadStartState(initUrl)
  }
).also { flow ->
  engine.addWebViewClient(object : WebViewClient() {
    override fun onPageFinished(view: WebView, url: String?) {
      flow.value = WebLoadSuccessState(url ?: "about:blank")
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
      if (url != null && !isWebUrlScheme(Url(url).protocol.name)) {
        /// TODO 显示询问对话框
        try {
          val ins = Intent(Intent.ACTION_VIEW, Uri.parse(url)).also {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              it.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
            }
          }
          val context = view.context
          context.packageManager.queryIntentActivities(ins, 0)
          context.startActivity(ins)
          engine.loadUrl("about:blank")
          return
        } catch (e: Exception) {
          println("QAQ onPageStarted error=${e.message}")
        }
      }
      flow.value = WebLoadStartState(url ?: "about:blank")
    }

    override fun onReceivedError(
      view: WebView, request: WebResourceRequest?, error: WebResourceError?,
    ) {
      // url必须相等，否则一些网页内资源异常会导致下一个网页无法正常加载
      if (request?.url?.toString()?.trimEnd('/') == view.url?.trimEnd('/')) {
        flow.value = WebLoadErrorState(view.url ?: "about:blank",
          error?.let { "[${it.errorCode}]${it.description}" } ?: "")
      }
    }
  })
}