package org.dweb_browser.helper.platform

import androidx.compose.ui.graphics.ImageBitmap

actual fun ByteArray.toImageBitmap() = skikoToImageBitmap()

actual fun ImageBitmap.toByteArray() = skikoToByteArray()