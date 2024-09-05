package org.dweb_browser.core.std.file.ext

import okio.Path.Companion.toPath
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.io.SystemFileSystem

actual fun loadByteChannelByPath(
  context: ResponseLocalFileBase, root: String, filePath: String, headers: PureHeaders,
): PureResponse {
  val fullFilePath = root + "/" + filePath.trimStart('/')
  return try {
    context.returnFile(SystemFileSystem, fullFilePath.toPath(), headers)
  } catch (e: Throwable) {
    context.returnNoFound(e.message)
  }
}