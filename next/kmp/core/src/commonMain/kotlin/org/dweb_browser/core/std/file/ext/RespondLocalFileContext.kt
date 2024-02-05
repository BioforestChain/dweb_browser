package org.dweb_browser.core.std.file.ext

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path
import okio.buffer
import org.dweb_browser.core.std.dns.debugFetchFile
import org.dweb_browser.helper.decodeURIComponent
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureBinary
import org.dweb_browser.pure.http.PureBinaryBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.io.SystemFileSystem
import org.dweb_browser.pure.io.toByteReadChannel

class RespondLocalFileContext(val request: PureClientRequest) {
  val filePath by lazy { request.url.encodedPath }
  private val mode = request.queryOrNull("mode") ?: "auto"
  val preferenceStream = mode == "stream"
  private fun asModePureBody(binary: PureBinary) =
    if (preferenceStream) PureStreamBody(binary) else PureBinaryBody(binary)

  private fun asModePureBody(binary: PureStream) = PureStreamBody(binary)

  fun returnFile(body: IPureBody, size: Long): PureResponse {
    val headers = PureHeaders()
    val extension = ContentType.fromFilePath(filePath)
    if (extension.isNotEmpty()) {
      headers.set("Content-Type", extension.first().toString())
    }
    headers.set("Content-Length", size.toString())
    return PureResponse(headers = headers, body = body)
  }

  fun returnFile(binary: PureBinary) = returnFile(asModePureBody(binary), binary.size.toLong())
  fun returnFile(stream: PureStream, size: Long) = returnFile(asModePureBody(stream), size)
  fun returnFile(byteChannel: ByteReadChannel, size: Long) =
    returnFile(PureStream(byteChannel), size)

  internal fun returnFile(fileSystem: FileSystem, filePath: Path, scope: CoroutineScope? = null) =
    returnFile(
      fileSystem.source(filePath).buffer()
        .run { if (scope != null) toByteReadChannel(scope) else toByteReadChannel() },
      SystemFileSystem.metadata(filePath).size ?: 0
    )

  fun returnFile(root: String, filePath: String) =
    loadByteChannelByPath(this, root.decodeURIComponent(), filePath.decodeURIComponent())

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