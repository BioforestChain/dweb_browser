package org.dweb_browser.helper.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.teamdev.jxbrowser.ui.Bitmap
import com.teamdev.jxbrowser.view.swing.graphics.BitmapImage

actual fun ByteArray.toImageBitmap(): ImageBitmap? = skikoToImageBitmap()

actual fun ImageBitmap.toByteArray() = skikoToByteArray()

fun Bitmap.toBufferedImage() = BitmapImage.toToolkit(this)
fun Bitmap.toImageBitmap() = toBufferedImage().toComposeImageBitmap()
