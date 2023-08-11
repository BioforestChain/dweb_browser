package org.dweb_browser.browserUI.network

import org.dweb_browser.microservice.sys.dns.httpFetch
import org.http4k.core.BodyMode
import org.http4k.core.Method
import org.http4k.core.Request

class HttpClient {

  suspend fun requestPath(
    path: String,
    method: Method = Method.GET,
    bodyMode: BodyMode = BodyMode.Memory,
    customRequest: (String, Method) -> Request = defaultRequest
  ) = httpFetch(customRequest(path, method))

  private val defaultRequest: (String, Method) -> Request = { url, method ->
    Request(method, url)
  }
}