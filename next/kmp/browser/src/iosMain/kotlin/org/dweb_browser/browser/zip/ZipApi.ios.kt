package org.dweb_browser.browser.zip

import ziplib.decompress as decompressByRust

actual fun decompress(zipFilePath: String, destPath: String) =
  decompressByRust(zipFilePath, destPath).toLong() == 0L