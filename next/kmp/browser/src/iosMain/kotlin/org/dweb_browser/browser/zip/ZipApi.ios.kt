package org.dweb_browser.browser.zip

import org.dweb_browser.ziplib.decompress as decompressByRust

actual fun decompress(zipFilePath: String, destPath: String) =
  decompressByRust(zipFilePath, destPath).toLong() == 0L