package org.dweb_browser.browser.scan

import com.google.zxing.BinaryBitmap
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.helper.PurePoint
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.platform.DeepLinkHook
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.abs


actual class ScanningController actual constructor(mmScope: CoroutineScope) {
  actual fun stop() {
  }

  // TODO 使用 openCV 来实现图像识别
  actual suspend fun recognize(data: ByteArray, rotation: Int): List<BarcodeResult> {
    if (data.isEmpty()) {
      return emptyList()
    }

    return CompletableDeferred<List<BarcodeResult>>().also { deferred ->
      runCatching {
        // 从byte array获取图片
        val bufferedImage = withContext(ioAsyncExceptionHandler) {
          ImageIO.read(ByteArrayInputStream(data))
        }
        val source = BufferedImageLuminanceSource(bufferedImage)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = QRCodeMultiReader()
        deferred.complete(
          try {
            reader.decodeMultiple(binaryBitmap).map { barcode ->
              // 返回的barcode中，resultPoints有三个标准点，也有可能存在4个，前三个是必定存在的。
              val topLeft = barcode.resultPoints[1]
              val bottomLeft = barcode.resultPoints[0]
              val topRight = barcode.resultPoints[2]
              val width = abs(topRight.x - topLeft.x)
              val height = abs(bottomLeft.y - topLeft.y)
              val boundingBox = PureRect(
                topLeft.x, topLeft.y, width, height
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
          } catch (e: Exception) { // 为了拦截解码异常的弹框
            emptyList()
          }
        )
      }.getOrElse {
        deferred.completeExceptionally(it)
      }
    }.await()
  }

  /**解析二维码时候的震动效果*/
  actual fun decodeHaptics() {
  }
}


/**
 * 打开扫码的结果
 */
actual fun openDeepLink(data: String, showBackground: Boolean): Boolean {
  // 下面判断的是否是 DeepLink，如果不是的话，判断是否是
  val deepLink = data.regexDeepLink() ?: run {
    if (data.isWebUrl()) {
      buildUrlString("dweb://openinbrowser") {
        parameters["url"] = data
      }
    } else {
      buildUrlString("dweb://search") {
        parameters["q"] = data
      }
    }
  }
  DeepLinkHook.instance.emitLink(deepLink)
  // Desktop.getDesktop().browse(URI.create(deepLink)) // 走系统 deeplink
  return true
}