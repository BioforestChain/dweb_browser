package org.dweb_browser.browser.scan

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun getBitmapCapture(): BitmapCapture = BitmapCaptureImpl()

class BitmapCaptureImpl : BitmapCapture {
  override suspend fun captureBitmap(size: Size): ImageBitmap {
    val bitmap =
      Bitmap.createBitmap(size.width.toInt(), size.height.toInt(), Bitmap.Config.ARGB_8888)
    // 在这里你可以使用 canvas 对象绘制需要捕获的内容
    return bitmap.asImageBitmap()
  }
}