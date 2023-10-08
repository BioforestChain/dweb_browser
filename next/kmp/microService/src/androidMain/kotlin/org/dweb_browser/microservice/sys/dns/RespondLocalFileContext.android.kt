package org.dweb_browser.microservice.sys.dns

import io.ktor.util.cio.toByteReadChannel
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.std.file.ext.RespondLocalFileContext
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

fun RespondLocalFileContext.returnAndroidFile(
  root: String, filePath: String = this.filePath
): PureResponse {
  val fullFilePath = root + File.separator + filePath.trimStart('/')
  return try {
    returnFile(File(fullFilePath))
  } catch (e: Throwable) {
    returnNoFound(e.message)
  }
}
