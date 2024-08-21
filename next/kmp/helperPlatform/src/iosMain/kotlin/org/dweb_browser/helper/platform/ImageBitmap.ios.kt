package org.dweb_browser.helper.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

actual fun ByteArray.toImageBitmap(): ImageBitmap? = skikoToImageBitmap()

actual fun ImageBitmap.toByteArray() = skikoToByteArray()

@OptIn(ExperimentalForeignApi::class)
fun UIImage.toImageBitmap(): ImageBitmap? {
  val bytes = UIImagePNGRepresentation(this) ?: return null

  val byteArray = ByteArray(bytes.length.toInt())
  byteArray.usePinned { memcpy(it.addressOf(0), bytes.bytes, bytes.length) }
  return Image.makeFromEncoded(byteArray).toComposeImageBitmap()
}
