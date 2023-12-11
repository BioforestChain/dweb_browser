package org.dweb_browser.helper.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

actual fun ByteArray.toImageBitmap(): ImageBitmap = toAndroidBitmap().asImageBitmap()

fun ByteArray.toAndroidBitmap(): Bitmap {
    return BitmapFactory.decodeByteArray(this, 0, size)
}

actual fun ImageBitmap.toByteArray(): ByteArray? {
    val stream = ByteArrayOutputStream()
    this.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 95, stream)
    return stream.toByteArray()
}