package org.dweb_browser.browser.scan

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.PurePoint
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.utf8String
import org.dweb_browser.sys.haptics.AndroidVibrate
import org.dweb_browser.sys.haptics.VibrateType

actual class ScanningController actual constructor(mmScope: CoroutineScope) {

  private val barcodeScanner = BarcodeScanning.getClient()
  actual fun stop() {
    barcodeScanner.close()
  }

  @OptIn(ExperimentalGetImage::class)
  actual suspend fun recognize(data: ByteArray, rotation: Int): List<BarcodeResult> {
    // 检查图像是否为空
    if (data.isEmpty()) {
      return listOf()
    }
    // 解码图像
    val bit = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return listOf()

    val image = InputImage.fromBitmap(bit, rotation)
    return process(image)
  }

  suspend fun recognize(bitmap: Bitmap, rotation: Int = 0) =
    process(InputImage.fromBitmap(bitmap, rotation))

  /**
   * 解析图片
   * @param inputImage 通过ImageAnalysis获取的图片
   * @param zoomPoint 原先会传递PreviewView，然后进行位置计算的，但是这边可以将计算值传递
   */
  suspend fun recognize(inputImage: InputImage, zoomPoint: Float) = runCatching {
    process(inputImage, zoomPoint)
  }.getOrElse {
    emptyList()
  }

  /**
   * 根据 zoomPoint 计算关键点位置
   */
  private fun pointToPurePoint(zoomPoint: Float, point: Point?) = point?.let {
    PurePoint(point.x * zoomPoint, point.y * zoomPoint)
  } ?: PurePoint.Zero

  /**
   * 根据Rect计算显示的PureRect
   */
  private fun rectToPureRect(zoomPoint: Float, rect: Rect?) = rect?.let {
    PureRect(
      rect.left * zoomPoint,
      rect.top * zoomPoint,
      rect.width() * zoomPoint,
      rect.height() * zoomPoint
    )
  } ?: PureRect.Zero

  private suspend fun process(image: InputImage, zoomPoint: Float = 1f): List<BarcodeResult> {
    val task = PromiseOut<List<BarcodeResult>>()
    barcodeScanner.process(image).addOnSuccessListener { barcodes ->
      task.resolve(barcodes.map { barcode ->
        val cornerPoints = barcode.cornerPoints
        BarcodeResult(
          data = barcode.rawBytes?.utf8String ?: "",
          boundingBox = rectToPureRect(zoomPoint, barcode.boundingBox),
          topLeft = pointToPurePoint(zoomPoint, cornerPoints?.get(0)),
          topRight = pointToPurePoint(zoomPoint, cornerPoints?.get(1)),
          bottomLeft = pointToPurePoint(zoomPoint, cornerPoints?.get(3)),
          bottomRight = pointToPurePoint(zoomPoint, cornerPoints?.get(2)),
        )
      })
    }.addOnFailureListener { err ->
      task.reject(err)
    }
    return task.waitPromise()
  }


  private val mVibrate = AndroidVibrate.mVibrate // 这里高度集成，没有调用HapticsNMM

  /**解析二维码时候的震动效果*/
  actual fun decodeHaptics() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      mVibrate.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    } else {
      mVibrate.vibrate(VibrateType.CLICK.oldSDKPattern, -1)
    }
  }
}


actual fun openDeepLink(data: String, showBackground: Boolean): Boolean {
  val context = getAppContextUnsafe()
  // 下盘的是否是 DeepLink，如果不是的话，判断是否是
  val deepLink = data.regexDeepLink() ?: run {
    when {
      data.isWebUrl() -> buildUrlString("dweb://openinbrowser") {
        parameters["url"] = data
      }

      else -> buildUrlString("dweb://search") {
        parameters["q"] = data
      }
    }
  }

  context.startActivity(Intent().apply {
    `package` = context.packageName
    action = Intent.ACTION_VIEW
    this.data = Uri.parse(deepLink)
    addCategory("android.intent.category.BROWSABLE")
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    putExtra("showBackground", showBackground)
  })
  return true
}