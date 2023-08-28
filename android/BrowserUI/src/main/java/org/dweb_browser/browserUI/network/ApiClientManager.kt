package org.dweb_browser.browserUI.network

import kotlinx.coroutines.withContext
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.microservice.sys.dns.httpFetch
import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode
import org.http4k.core.Method
import org.http4k.core.Request

class HttpClient {
  private val mClientStream =
    ApacheClient(requestBodyMode = BodyMode.Stream, responseBodyMode = BodyMode.Stream)

  suspend fun requestPath(
    path: String, method: Method = Method.GET
  ) = withContext(ioAsyncExceptionHandler) {
    httpFetch(Request(method, path))
  }

  suspend fun download(
    path: String, request: (() -> Request)? = null
  ) = withContext(ioAsyncExceptionHandler) {
    mClientStream(request?.let { it() } ?: Request(Method.GET, path))
  }
}