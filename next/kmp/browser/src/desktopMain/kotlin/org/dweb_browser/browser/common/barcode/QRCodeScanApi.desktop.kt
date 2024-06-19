package org.dweb_browser.browser.common.barcode

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import kotlinx.io.IOException
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.platform.DeepLinkHook
import kotlin.math.abs

/**
 * 振动，响铃
 */
actual fun beepAudio() {
  WARNING("Not yet implemented beepAudio")
}

actual fun decoderImage(
  imageBitmap: ImageBitmap,
  onSuccess: (QRCodeDecoderResult) -> Unit,
  onFailure: (Exception) -> Unit,
) {
  try {
    val bufferedImage = imageBitmap.toAwtImage()
    val source = BufferedImageLuminanceSource(bufferedImage)
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    val reader = QRCodeMultiReader()
    val result = reader.decodeMultiple(bitmap)
    val listRect: MutableList<QRCodeDecoderResult.QRCode> = mutableListOf()
    result.forEach { barcode ->
      // 通常二维码是返回3个坐标，也就是左上，右上，左下的坐标，也可能存在4个坐标，也就是右下的坐标
      val centerX = (maxOf(
        barcode.resultPoints[0].x, barcode.resultPoints[1].x, barcode.resultPoints[2].x
      ) + minOf(
        barcode.resultPoints[0].x, barcode.resultPoints[1].x, barcode.resultPoints[2].x
      )) / 2
      val centerY = (maxOf(
        barcode.resultPoints[0].y, barcode.resultPoints[1].y, barcode.resultPoints[2].y
      ) + minOf(
        barcode.resultPoints[0].y, barcode.resultPoints[1].y, barcode.resultPoints[2].y
      )) / 2
      println("decodeImage => center=($centerX, $centerY), ${barcode.resultPoints.size}, ${barcode.resultPoints.contentToString()}")
      listRect.add(
        QRCodeDecoderResult.QRCode(
          org.dweb_browser.helper.PureRect(x = centerX, y = centerY), barcode.text
        )
      )
    }
    val qrCodeDecoderResult = QRCodeDecoderResult(imageBitmap, imageBitmap, listRect)
    onSuccess(qrCodeDecoderResult)
  } catch (e: NotFoundException) {
    onFailure(e)
  } catch (e: IOException) {
    onFailure(e)
  }
}

/**
 * 计算二维码的位置
 */
actual fun transformPoint(
  x: Int, y: Int, srcWidth: Int, srcHeight: Int, destWidth: Int, destHeight: Int, isAlarm: Boolean,
): QRCodeDecoderResult.Point {
  val widthRatio = destWidth * 1.0f / srcWidth
  val heightRatio = destHeight * 1.0f / srcHeight

  return if (isAlarm) {
    val ratio = widthRatio.coerceAtMost(heightRatio)
    val left = abs(srcWidth * ratio - destWidth) / 2
    val top = abs(srcHeight * ratio - destHeight) / 2
    QRCodeDecoderResult.Point(x * ratio + left, y * ratio + top)
  } else {
    val ratio = widthRatio.coerceAtLeast(heightRatio)
    val left = abs(srcWidth * ratio - destWidth) / 2
    val top = abs(srcHeight * ratio - destHeight) / 2
    QRCodeDecoderResult.Point(x * ratio - left, y * ratio - top)
  }
}

/**
 * 打开扫码的结果
 */
actual fun openDeepLink(data: String, showBackground: Boolean): Boolean {
  // 下面判断的是否是 DeepLink，如果不是的话，判断是否是
  val deepLink = data.regexDeepLink() ?: run {
    if (data.isWebUrl()) {
      "dweb://openinbrowser?url=${data.encodeURIComponent()}"
    } else {
      "dweb://search?q=${data.encodeURIComponent()}"
    }
  }
  DeepLinkHook.deepLinkHook.emitOnInit(deepLink)
  // Desktop.getDesktop().browse(URI.create(deepLink)) // 走系统 deeplink
  return false
}