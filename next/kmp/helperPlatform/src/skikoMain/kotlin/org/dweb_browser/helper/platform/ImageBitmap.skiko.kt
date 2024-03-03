package org.dweb_browser.helper.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

fun ByteArray.skikoToImageBitmap(): ImageBitmap =
  Image.makeFromEncoded(this).toComposeImageBitmap()

fun ImageBitmap.skikoToByteArray(): ByteArray? =
  Image.makeFromBitmap(this.asSkiaBitmap()).encodeToData()?.bytes