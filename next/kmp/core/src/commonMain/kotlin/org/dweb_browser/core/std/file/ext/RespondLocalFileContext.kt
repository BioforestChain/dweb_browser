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
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureRequest
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.io.toByteReadChannel

open class ResponseLocalFileBase(filePath: String? = null, preferenceStream: Boolean? = null) {
  open val filePath = filePath ?: ""
  open val preferenceStream = preferenceStream ?: false
  private fun asModePureBody(binary: PureBinary) =
    if (preferenceStream) PureStreamBody(binary) else PureBinaryBody(binary)

  private fun asModePureBody(binary: PureStream) = PureStreamBody(binary)
  fun returnFile(body: IPureBody, headers: PureHeaders = PureHeaders()): PureResponse {
    if (!headers.has("Content-Type")) {
      (ContentType.fromFilePath(filePath).firstOrNull()?.toString())?.also {
        headers.set("Content-Type", it)
      }
    }
//    if (!headers.has("Content-Length")) {
//    if (size != null) {
//      headers.set("Content-Length", size.toString())
//    }
    return PureResponse(headers = headers, body = body)
  }

  fun returnFile(binary: PureBinary, headers: PureHeaders = PureHeaders()) =
    returnFile(
      asModePureBody(binary),
      headers.apply { headers.setContentLength(binary.size) })

  fun returnFile(stream: PureStream, headers: PureHeaders = PureHeaders()) =
    returnFile(asModePureBody(stream), headers)

  fun returnFile(byteChannel: ByteReadChannel, headers: PureHeaders = PureHeaders()) =
    returnFile(PureStream(byteChannel), headers)

  internal fun returnFile(
    fileSystem: FileSystem,
    filePath: Path,
    headers: PureHeaders = PureHeaders(),
    scope: CoroutineScope? = null,
  ) = returnFile(
    fileSystem.source(filePath).buffer()
      .run { if (scope != null) toByteReadChannel(scope) else toByteReadChannel() },
    headers.apply {
      headers.setContentLength(fileSystem.metadata(filePath).size ?: 0)
    },
  )

  fun returnFile(root: String, filePath: String, headers: PureHeaders = PureHeaders()) =
    loadByteChannelByPath(this, root.decodeURIComponent(), filePath.decodeURIComponent(), headers)

  fun returnNoFound(message: String? = null): PureResponse {
    debugFetchFile("NO-FOUND-FILE", filePath)
    return PureResponse(
      HttpStatusCode.NotFound, body = PureStringBody(message ?: "the file($filePath) not found.")
    )
  }

  fun returnNext() = null
}

class RespondLocalFileContext(val request: PureRequest) : ResponseLocalFileBase() {
  override val filePath by lazy { request.url.encodedPath }
  private val mode = request.queryOrNull("mode") ?: "auto"
  override val preferenceStream = mode == "stream"

  companion object {
    suspend fun PureRequest.respondLocalFile(respond: suspend RespondLocalFileContext.() -> PureResponse?) =
      if (url.protocol.name == "file" && url.host == "") {
        runCatching { RespondLocalFileContext(this).respond() }.getOrElse {
          // jvm FileNotFoundException
          if (it.message?.contains("No such file or directory") == true) {
            PureResponse(HttpStatusCode.NotFound)
          } else null
        }
      } else null
  }
}

expect fun loadByteChannelByPath(
  context: ResponseLocalFileBase,
  root: String,
  filePath: String,
  headers: PureHeaders,
): PureResponse