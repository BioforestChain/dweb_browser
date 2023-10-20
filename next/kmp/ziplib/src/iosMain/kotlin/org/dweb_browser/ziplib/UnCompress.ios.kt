package org.dweb_browser.ziplib

import kotlinx.cinterop.ExperimentalForeignApi
import miniz.zip_extract


@OptIn(ExperimentalForeignApi::class)
actual fun unCompress(zipFilePath: String, destPath: String) : Boolean = zip_extract(zipFilePath, destPath, null, null) == 0
