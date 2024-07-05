package org.dweb_browser.dwebview.engine

import android.content.Intent
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ktor.http.ContentType
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.base.isWebUrlScheme
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import java.io.InputStream

class DWebOverwriteRequest(val engine: DWebViewEngine) : WebViewClient() {
  override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
    if (!isWebUrlScheme(request.url.scheme ?: "http")) {
      if (request.url.scheme == "dweb") {
        engine.lifecycleScope.launch {
          engine.remoteMM.nativeFetch(request.url.toString())
        }
        return true
      }
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
  /// TODO 因为 Chrome 错误地设置了 InputStream

  private val WebResourceRequest.shouldIntercept
    get() = engine.options.androidInterceptGetRequest && method == "GET" && url.host?.endsWith(".dweb") == true && url.scheme == "https"

  override fun shouldInterceptRequest(
    view: WebView, request: WebResourceRequest,
  ): WebResourceResponse? {
    // 转发请求
    // TODO 可以使用开关来进行拦截
    if (request.url.scheme == "dweb" || request.shouldIntercept) {
      val response = runBlocking(ioAsyncExceptionHandler) {
        engine.remoteMM.nativeFetch(
          PureClientRequest(
            request.url.toString(), PureMethod.GET, PureHeaders(request.requestHeaders)
          )
        )
      }

      val contentType = response.headers.get("Content-Type")
        ?.let { ContentType.parse(it) } // (response.headers.get("Content-Type") ?: "").split(';', limit = 2)

      val statusCode = response.status.value
      debugDWebView("dwebProxy end", "[$statusCode]${request.url}  contentType=>$contentType")
      if (statusCode in 301..399) {
        return super.shouldInterceptRequest(view, request)
      }
      val inputStreamSource =
        response.body.toPureStream().getReader("DwebView shouldInterceptRequest").toInputStream()
      val safeContentLength = response.headers.get("Content-Length")?.toInt()
      val inputStreamProxy = object : InputStream() {
        override fun available(): Int {
          /// 这个如果错误地给出去，chromium会把它当成 Content-Length 写入到头部，这回导致浏览器内部的解码器出错
          return safeContentLength ?: super.available()
        }

        override fun read(): Int {
          return inputStreamSource.read()
        }

        override fun read(b: ByteArray?, off: Int, len: Int): Int {
          return inputStreamSource.read(b!!, off, len)
        }

        override fun close() {
          inputStreamSource.close()
        }
      }
      return WebResourceResponse(
        contentType?.let { "${it.contentType}/${it.contentSubtype}" },
        contentType?.parameter("charset"),
        response.status.value,
        response.status.description,
        response.headers.toMap()
          .let { it - "Content-Type" - "Content-Length" }, // 修复 content-type 问题
        inputStreamProxy,
      )
    }
    return super.shouldInterceptRequest(view, request)
  }

  override fun onReceivedHttpError(
    view: WebView?,
    request: WebResourceRequest?,
    errorResponse: WebResourceResponse?,
  ) {
    WARNING("onReceivedHttpError: [${request?.method}] ${request?.url}")
    WARNING("status: ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase}")
    WARNING("body: ${errorResponse?.data}")
    super.onReceivedHttpError(view, request, errorResponse)
  }
}
