package org.dweb_browser.helper.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.teamdev.jxbrowser.ui.Bitmap
import com.teamdev.jxbrowser.view.compose.graphics.toBufferedImage

actual fun ByteArray.toImageBitmap(): ImageBitmap? = skikoToImageBitmap()

actual fun ImageBitmap.toByteArray() = skikoToByteArray()

fun Bitmap.toImageBitmap() = toBufferedImage().toComposeImageBitmap()
