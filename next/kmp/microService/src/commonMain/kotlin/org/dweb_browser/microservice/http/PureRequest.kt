package org.dweb_browser.microservice.http

import io.ktor.http.Url
import io.ktor.utils.io.core.Closeable
import kotlinx.serialization.Serializable
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcMethod

@Serializable
class PureRequest(
  var url: String,
  var method: IpcMethod,
  var headers: IpcHeaders = IpcHeaders(),
  var body: IPureBody = IPureBody.Empty
) : Closeable {

  var parsedUrl: Url
    get() = Url(url)
    set(value) {
      if(value.toString() != url) {
        url = value.toString()
      }
    }

  val safeUrl: Url get() = parsedUrl

  override fun close() {
    (body as? PureStreamBody)?.stream?.close()
  }
}