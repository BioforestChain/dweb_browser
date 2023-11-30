package org.dweb_browser.dwebview.engine

import android.content.Intent
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.base.isWebUrlScheme
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.ioAsyncExceptionHandler

class DWebRequestResponse(val engine: DWebViewEngine) : WebViewClient() {
  override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
    if (!isWebUrlScheme(request.url.scheme ?: "http")) {
      /// TODO 显示询问对话框
      try {
        val ins = Intent(Intent.ACTION_VIEW, request.url).also {
          it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            it.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
          }
        }
        val context = view.context
        context.packageManager.queryIntentActivities(ins, 0)
        context.startActivity(ins)
      } catch (_: Exception) {
      }
      return true
    }
    return super.shouldOverrideUrlLoading(view, request)
  }

  override fun shouldInterceptRequest(
    view: WebView, request: WebResourceRequest
  ): WebResourceResponse? {
    // 转发请求
    if (request.method == "GET" && ((request.url.host?.endsWith(".dweb") == true) || (request.url.scheme == "dweb"))) {
      val response = runBlocking(ioAsyncExceptionHandler) {
        engine.remoteMM.nativeFetch(
          PureRequest(
            request.url.toString(), IpcMethod.GET, IpcHeaders(request.requestHeaders)
          )
        )
      }

      val contentType = (response.headers.get("Content-Type") ?: "").split(';', limit = 2)

      val statusCode = response.status.value
      debugDWebView("dwebProxy end", "[$statusCode]${request.url}")
      if (statusCode in 301..399) {
        return super.shouldInterceptRequest(view, request)
      }
      return WebResourceResponse(
        contentType.firstOrNull(),
        contentType.lastOrNull(),
        response.status.value,
        response.status.description,
        response.headers.toMap().let { it - "Content-Type" }, // 修复 content-type 问题
        response.body.toPureStream().getReader("DwebView shouldInterceptRequest").toInputStream(),
      )
    }
    return super.shouldInterceptRequest(view, request)
  }
}
