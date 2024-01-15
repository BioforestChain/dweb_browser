package org.dweb_browser.pure.io

import okio.FileSystem
import okio.NodeJsFileSystem

actual val SystemFileSystem: FileSystem = NodeJsFileSystem