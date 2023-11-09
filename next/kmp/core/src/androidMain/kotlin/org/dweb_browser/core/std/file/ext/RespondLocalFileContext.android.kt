package org.dweb_browser.core.std.file.ext

import io.ktor.util.cio.toByteReadChannel
import org.dweb_browser.core.http.PureResponse
import java.io.File
import java.io.InputStream

fun RespondLocalFileContext.returnFile(inputStream: InputStream) =
  returnFile(inputStream.toByteReadChannel())

fun RespondLocalFileContext.returnFile(file: File) =
  if (preferenceStream) returnFile(file.inputStream()) else returnFile(file.readBytes())

fun String.parseToDirnameAndBasename(): Pair<String, String> {
  var basename = ""
  var dirname = ""
  val pathSegments = trim('/').split('/').filter { it.isNotEmpty() }.toMutableList()
  when (pathSegments.size) {
    0 -> {}
    1 -> {
      dirname = ""
      basename = pathSegments.first()
    }

    else -> {
      basename = pathSegments.removeLast()
      dirname = pathSegments.joinToString("/")
    }
  }
  return Pair(dirname, basename)
}

actual fun loadByteChannelByPath(
  context: RespondLocalFileContext, root: String, filePath: String
): PureResponse {
  val fullFilePath = root + File.separator + filePath.trimStart('/')
  return try {
    context.returnFile(File(fullFilePath))
  } catch (e: Throwable) {
    context.returnNoFound(e.message)
  }
}