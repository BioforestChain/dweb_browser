package org.dweb_browser.pure.io

import okio.FileSystem

actual val SystemFileSystem: FileSystem get() = throw Exception("FileSystem no support in browser yet")