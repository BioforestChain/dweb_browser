package org.dweb_browser.browser.scan

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.viewfinder.compose.CoordinateTransformer
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
import org.dweb_browser.helper.toPoint
import org.dweb_browser.helper.toRect
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

  suspend fun recognize(image: InputImage) = process(image)

  @OptIn(ExperimentalGetImage::class)
//  suspend fun recognize(imageProxy: ImageProxy, coordinateTransform: CoordinateTransformer) =
  suspend fun recognize(imageProxy: ImageProxy) =
    runCatching {
      val inputImage = InputImage.fromBitmap(imageProxy.toBitmap(), 0)
      process(inputImage)
    }.getOrElse {
      emptyList()
    }

  private suspend fun process(
    image: InputImage, coordinateTransform: CoordinateTransformer? = null,
  ): List<BarcodeResult> {
    val task = PromiseOut<List<BarcodeResult>>()
    barcodeScanner.process(image).addOnSuccessListener { barcodes ->
      task.resolve(barcodes.map { barcode ->
        val cornerPoints = barcode.cornerPoints
//        if (matrix !== null) {
//          val boundingBox = RectF(barcode.boundingBox)
//          matrix.mapRect(boundingBox)
//          // 转换顶点坐标
//          val topLeft = cornerPoints?.get(0)?.let { point -> mapPointToPreviewView(matrix, point) }
//            ?: PurePoint.Zero
//          val topRight = cornerPoints?.get(1)?.let { point -> mapPointToPreviewView(matrix, point) }
//            ?: PurePoint.Zero
//          val bottomLeft =
//            cornerPoints?.get(3)?.let { point -> mapPointToPreviewView(matrix, point) }
//              ?: PurePoint.Zero
//          val bottomRight =
//            cornerPoints?.get(2)?.let { point -> mapPointToPreviewView(matrix, point) }
//              ?: PurePoint.Zero
//          BarcodeResult(
//            data = barcode.rawBytes?.utf8String ?: "",
//            boundingBox = boundingBox.toRect(),
//            topLeft = topLeft,
//            topRight = topRight,
//            bottomLeft = bottomLeft,
//            bottomRight = bottomRight,
//          )
//        } else {
          BarcodeResult(
            data = barcode.rawBytes?.utf8String ?: "",
            boundingBox = barcode.boundingBox?.toRect() ?: PureRect.Zero,
            topLeft = cornerPoints?.get(0)?.toPoint() ?: PurePoint.Zero,
            topRight = cornerPoints?.get(1)?.toPoint() ?: PurePoint.Zero,
            bottomLeft = cornerPoints?.get(3)?.toPoint() ?: PurePoint.Zero,
            bottomRight = cornerPoints?.get(2)?.toPoint() ?: PurePoint.Zero,
          )
//        }
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