package org.dweb_browser.core.std.file.ext

import org.dweb_browser.pure.http.PureHeaders

actual fun loadByteChannelByPath(
  context: ResponseLocalFileBase, root: String, filePath: String, headers: PureHeaders,
) = jvmLoadByteChannelByPath(context, root, filePath, headers)