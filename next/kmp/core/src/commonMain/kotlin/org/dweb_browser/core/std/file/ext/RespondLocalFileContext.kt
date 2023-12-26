package org.dweb_browser.core.std.file.ext

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.utils.io.ByteReadChannel
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureBinary
import org.dweb_browser.pure.http.PureBinaryBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.core.std.dns.debugFetchFile
import org.dweb_browser.helper.decodeURIComponent

class RespondLocalFileContext(val request: PureClientRequest) {
  val filePath by lazy { request.url.encodedPath }
  private val mode = request.queryOrNull("mode") ?: "auto"
  val preferenceStream = mode == "stream"
  private fun asModePureBody(binary: PureBinary) =
    if (preferenceStream) PureStreamBody(binary) else PureBinaryBody(binary)

  private fun asModePureBody(binary: PureStream) = PureStreamBody(binary)

  fun returnFile(body: IPureBody): PureResponse {
    val headers = PureHeaders()
    val extension = ContentType.fromFilePath(filePath)
    if (extension.isNotEmpty()) {
      headers.init("Content-Type", extension.first().toString())
    }
    return PureResponse(headers = headers, body = body)
  }

  fun returnFile(binary: PureBinary) = returnFile(asModePureBody(binary))
  fun returnFile(stream: PureStream) = returnFile(asModePureBody(stream))
  fun returnFile(byteChannel: ByteReadChannel) = returnFile(PureStream(byteChannel))
  fun returnFile(root: String, filePath: String) = loadByteChannelByPath(this, root.decodeURIComponent(), filePath.decodeURIComponent())

  fun returnNoFound(message: String? = null): PureResponse {
    debugFetchFile("NO-FOUND-FILE", filePath)
    return PureResponse(
      HttpStatusCode.NotFound, body = PureStringBody(message ?: "the file($filePath) not found.")
    )
  }

  fun returnNext() = null

  companion object {
    suspend fun PureClientRequest.respondLocalFile(respond: suspend RespondLocalFileContext.() -> PureResponse?) =
      if (url.protocol.name == "file" && url.host == "") {
        RespondLocalFileContext(this).respond()
      } else null
  }
}

expect fun loadByteChannelByPath(
  context: RespondLocalFileContext,
  root: String,
  filePath: String
): PureResponse