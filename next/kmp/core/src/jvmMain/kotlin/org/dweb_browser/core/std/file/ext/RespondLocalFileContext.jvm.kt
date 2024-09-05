package org.dweb_browser.core.std.file.ext

import io.ktor.util.cio.toByteReadChannel
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureResponse
import java.io.File
import java.io.InputStream

fun ResponseLocalFileBase.returnFile(inputStream: InputStream, headers: PureHeaders) =
  returnFile(inputStream.toByteReadChannel(), headers)

fun ResponseLocalFileBase.returnFile(file: File, headers: PureHeaders) =
  if (preferenceStream) returnFile(
    file.inputStream(),
    headers.apply {
      headers.setContentLength(file.length())
    }
  ) else returnFile(file.readBytes(), headers)


fun jvmLoadByteChannelByPath(
  context: ResponseLocalFileBase, root: String, filePath: String, headers: PureHeaders,
): PureResponse {
  val fullFilePath = root + File.separator + filePath.trimStart('/')
  return try {
    context.returnFile(File(fullFilePath), headers)
  } catch (e: Throwable) {
    context.returnNoFound(e.message)
  }
}