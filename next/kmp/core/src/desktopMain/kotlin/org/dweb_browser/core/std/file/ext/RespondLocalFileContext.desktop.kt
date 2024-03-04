package org.dweb_browser.core.std.file.ext

actual fun loadByteChannelByPath(
  context: RespondLocalFileContext, root: String, filePath: String
) = jvmLoadByteChannelByPath(context, root, filePath)