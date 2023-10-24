package org.dweb_browser.browser.zip

import org.dweb_browser.helper.ZipUtil

actual fun decompress(zipFilePath: String, destPath: String) = ZipUtil.ergodicDecompress(zipFilePath, destPath)