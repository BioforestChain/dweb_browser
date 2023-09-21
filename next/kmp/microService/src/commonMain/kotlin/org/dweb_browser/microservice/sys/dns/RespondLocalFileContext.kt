package org.dweb_browser.microservice.sys.dns

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.utils.io.ByteReadChannel
import org.dweb_browser.microservice.http.IPureBody
import org.dweb_browser.microservice.http.PureBinary
import org.dweb_browser.microservice.http.PureBinaryBody
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStream
import org.dweb_browser.microservice.http.PureStreamBody
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.ipc.helper.IpcHeaders


class RespondLocalFileContext(val request: PureRequest) {
  val filePath by lazy { request.url.encodedPath }
  private val mode = request.query("mode") ?: "auto"
  val preferenceStream = mode == "stream"
  private fun asModePureBody(binary: PureBinary) =
    if (preferenceStream) PureStreamBody(binary) else PureBinaryBody(binary)

  private fun asModePureBody(binary: PureStream) = PureStreamBody(binary)

  fun returnFile(body: IPureBody): PureResponse {
    val headers = IpcHeaders()
    val extension = ContentType.fromFilePath(filePath)
    if (extension.isNotEmpty()) {
      headers.init("Content-Type", extension.first().toString())
    }
    return PureResponse(headers = headers, body = body)
  }

  fun returnFile(binary: PureBinary) = returnFile(asModePureBody(binary))
  fun returnFile(stream: PureStream) = returnFile(asModePureBody(stream))
  fun returnFile(byteChannel: ByteReadChannel) = returnFile(PureStream(byteChannel))
  fun returnNoFound(message: String? = null): PureResponse {
    debugFetchFile("NO-FOUND-FILE", filePath)
    return PureResponse(
      HttpStatusCode.NotFound, body = PureStringBody(message ?: "the file($filePath) not found.")
    )
  }

  fun returnNext() = null

  companion object {
    suspend fun PureRequest.respondLocalFile(respond: suspend RespondLocalFileContext.() -> PureResponse?) =
      if (url.protocol.name == "file" && url.host == "") {
        RespondLocalFileContext(this).respond()
      } else null
  }
}
