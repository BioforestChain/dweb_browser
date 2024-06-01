package org.dweb_browser.core.std.file.ext

import io.ktor.util.cio.toByteReadChannel
import org.dweb_browser.pure.http.PureResponse
import java.io.File
import java.io.InputStream

fun ResponseLocalFileBase.returnFile(inputStream: InputStream, size: Long) =
  returnFile(inputStream.toByteReadChannel(), size)

fun ResponseLocalFileBase.returnFile(file: File) =
  if (preferenceStream) returnFile(
    file.inputStream(),
    file.length()
  ) else returnFile(file.readBytes())


fun jvmLoadByteChannelByPath(
  context: ResponseLocalFileBase, root: String, filePath: String
): PureResponse {
  val fullFilePath = root + File.separator + filePath.trimStart('/')
  return try {
    context.returnFile(File(fullFilePath))
  } catch (e: Throwable) {
    context.returnNoFound(e.message)
  }
}