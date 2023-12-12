package org.dweb_browser.browser.zip

actual fun decompress(zipFilePath: String, destPath: String) = ZipUtil.ergodicDecompress(zipFilePath, destPath, false)