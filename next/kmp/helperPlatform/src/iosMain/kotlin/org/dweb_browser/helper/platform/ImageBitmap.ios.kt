package org.dweb_browser.helper.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun ByteArray.toImageBitmap(): ImageBitmap =
    Image.makeFromEncoded(this).toComposeImageBitmap()

actual fun ImageBitmap.toByteArray(): ByteArray? =
    Image.makeFromBitmap(this.asSkiaBitmap()).encodeToData()?.bytes