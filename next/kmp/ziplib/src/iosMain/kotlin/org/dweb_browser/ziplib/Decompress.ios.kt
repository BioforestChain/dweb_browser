package org.dweb_browser.ziplib

import kotlinx.cinterop.ExperimentalForeignApi
import miniz.zip_extract


@OptIn(ExperimentalForeignApi::class)
actual fun decompress(zipFilePath: String, destPath: String) : Boolean = zip_extract(zipFilePath, destPath, null, null) == 0
