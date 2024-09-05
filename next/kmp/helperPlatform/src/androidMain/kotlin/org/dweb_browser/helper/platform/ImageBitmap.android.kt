package org.dweb_browser.helper.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

actual fun ByteArray.toImageBitmap(): ImageBitmap? = toAndroidBitmap()?.asImageBitmap()

fun ByteArray.toAndroidBitmap(): Bitmap? {
  return try {
//    isEmpty().trueAlso { return null }
    BitmapFactory.decodeByteArray(this, 0, size)
  } catch (e: Exception) {
    null
  }
}

fun ByteBuffer.toByteArray(): ByteArray {
  rewind()    // Rewind the buffer to zero
  val data = ByteArray(remaining())
  get(data)   // Copy the buffer into a byte array
  return data // Return the byte array
}

actual fun ImageBitmap.toByteArray(): ByteArray? {
  val stream = ByteArrayOutputStream()
  this.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 95, stream)
  return stream.toByteArray()
}