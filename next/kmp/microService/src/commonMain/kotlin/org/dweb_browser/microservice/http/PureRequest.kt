package org.dweb_browser.microservice.http

import io.ktor.http.Url
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcMethod
import org.dweb_browser.microservice.ipc.helper.IpcRequest

data class PureRequest(
  val url: String,
  val method: IpcMethod,
  val headers: IpcHeaders = IpcHeaders(),
  val body: IPureBody = IPureBody.Empty
) {

  val parsedUrl by lazy { Url(url) }

  val safeUrl: Url get() = parsedUrl
}

fun IpcRequest.toPure() = PureRequest(url, method, headers, body.raw)