package org.dweb_browser.helper

import okio.FileSystem
import okio.NodeJsFileSystem

actual val SystemFileSystem: FileSystem = NodeJsFileSystem