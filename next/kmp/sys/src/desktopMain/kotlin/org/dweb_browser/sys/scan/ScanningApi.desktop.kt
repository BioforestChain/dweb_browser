package org.dweb_browser.sys.scan

import com.google.zxing.BinaryBitmap
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.PurePoint
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.ioAsyncExceptionHandler
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.abs

actual class ScanningManager actual constructor() {
  actual fun stop() {
  }

  // TODO 使用 openCV 来实现图像识别
  actual suspend fun recognize(img: ByteArray, rotation: Int): List<BarcodeResult> {
    val task = PromiseOut<List<BarcodeResult>>()
    try {
      // 从byte array获取图片
      val bufferedImage = withContext(ioAsyncExceptionHandler) {
        ImageIO.read(ByteArrayInputStream(img))
      }
      val source = BufferedImageLuminanceSource(bufferedImage)
      val bitmap = BinaryBitmap(HybridBinarizer(source))
      val reader = QRCodeMultiReader()
      task.resolve(
        reader.decodeMultiple(bitmap).map { barcode ->
          // 返回的barcode中，resultPoints有三个标准点，也有可能存在4个，前三个是必定存在的。
          val topLeft = barcode.resultPoints[1]
          val bottomLeft = barcode.resultPoints[0]
          val topRight = barcode.resultPoints[2]
          val boundingBox = PureRect(
            topLeft.x, topLeft.y, abs(topRight.x - topLeft.x), abs(bottomLeft.y - topLeft.y)
          )

          BarcodeResult(
            data = barcode.text,
            boundingBox = boundingBox,
            topLeft = PurePoint(topLeft.x, topLeft.y),
            topRight = PurePoint(topRight.x, topRight.y),
            bottomLeft = PurePoint(bottomLeft.x, bottomLeft.y),
            bottomRight = PurePoint(topRight.x, bottomLeft.y)
          )
        }
      )
    } catch (e: Exception) {
      task.reject(e)
    }
    return task.waitPromise()
  }
}