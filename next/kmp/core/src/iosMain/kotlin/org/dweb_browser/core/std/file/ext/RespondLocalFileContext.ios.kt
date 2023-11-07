package org.dweb_browser.core.std.file.ext

import org.dweb_browser.core.http.PureResponse

actual fun loadByteChannelByPath(
  context: RespondLocalFileContext, root: String, filePath: String
): PureResponse {
  return context.returnNoFound()
}