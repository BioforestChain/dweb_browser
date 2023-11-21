package org.dweb_browser.core.std.file.ext

import okio.Path.Companion.toPath
import okio.buffer
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.helper.SystemFileSystem
import org.dweb_browser.helper.toByteReadChannel

actual fun loadByteChannelByPath(
  context: RespondLocalFileContext, root: String, filePath: String
): PureResponse {
  val fullFilePath = root + "/" + filePath.trimStart('/')
  return try {
    context.returnFile(SystemFileSystem.source(fullFilePath.toPath()).buffer().toByteReadChannel())
  } catch (e: Throwable) {
    context.returnNoFound(e.message)
  }
}