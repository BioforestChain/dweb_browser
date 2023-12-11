package org.dweb_browser.helper.platform
import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.toImageBitmap(): ImageBitmap

expect fun ImageBitmap.toByteArray(): ByteArray?