package org.dweb_browser.core.std.file.ext

import okio.Path.Companion.toPath
import okio.buffer
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.io.SystemFileSystem
import org.dweb_browser.pure.io.toByteReadChannel

actual fun loadByteChannelByPath(
  context: RespondLocalFileContext, root: String, filePath: String
): PureResponse {
  val fullFilePath = root + "/" + filePath.trimStart('/')
  return try {
    context.returnFile(SystemFileSystem, fullFilePath.toPath())
  } catch (e: Throwable) {
    context.returnNoFound(e.message)
  }
}