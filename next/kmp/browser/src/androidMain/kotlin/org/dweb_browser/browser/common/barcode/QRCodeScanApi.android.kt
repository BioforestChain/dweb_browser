package org.dweb_browser.browser.common.barcode

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.isWebUrl
import kotlin.math.abs

actual fun beepAudio() {
  (getAppContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager)
    .playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
}

actual fun decoderImage(
  imageBitmap: ImageBitmap, onSuccess: (QRCodeDecoderResult) -> Unit, onFailure: (Exception) -> Unit
) {
  val inputImage =
    InputImage.fromBitmap(imageBitmap.asAndroidBitmap(), android.view.Surface.ROTATION_0)
  BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
  ).process(inputImage).addOnSuccessListener { result ->
    if (result.isEmpty()) {
      onFailure(Exception("No Found QR Code"))
    } else {
      val listRect: MutableList<QRCodeDecoderResult.QRCode> = mutableListOf()
      result.forEach { barcode ->
        listRect.add(
          QRCodeDecoderResult.QRCode(
            org.dweb_browser.helper.PureRect(
              x = barcode.boundingBox?.centerX()?.toFloat() ?: 0f,
              y = barcode.boundingBox?.centerY()?.toFloat() ?: 0f
            ), null
          )
        )
      }
      val lastImage = inputImage.bitmapInternal?.asImageBitmap()
      val qrCodeDecoderResult = QRCodeDecoderResult(imageBitmap, lastImage, listRect)
      onSuccess(qrCodeDecoderResult)
    }
  }.addOnFailureListener { e -> onFailure(e) }
}


actual fun transformPoint(
  x: Int, y: Int, srcWidth: Int, srcHeight: Int, destWidth: Int, destHeight: Int, isFit: Boolean
): QRCodeDecoderResult.Point {
  val widthRatio = destWidth * 1.0f / srcWidth
  val heightRatio = destHeight * 1.0f / srcHeight
  return if (isFit) { //宽或高自适应铺满
    val ratio = widthRatio.coerceAtMost(heightRatio)
    val left = abs(srcWidth * ratio - destWidth) / 2
    val top = abs(srcHeight * ratio - destHeight) / 2
    QRCodeDecoderResult.Point(x * ratio + left, y * ratio + top)
  } else { //填充铺满（可能会出现裁剪）
    val ratio = widthRatio.coerceAtLeast(heightRatio)
    val left = abs(srcWidth * ratio - destWidth) / 2
    val top = abs(srcHeight * ratio - destHeight) / 2
    QRCodeDecoderResult.Point(x * ratio - left, y * ratio - top)
  }
}

actual fun openDeepLink(data: String, showBackground: Boolean): Boolean {
  val context = getAppContext()
  // 下盘的是否是 DeepLink，如果不是的话，判断是否是
  val deepLink = data.regexDeepLink() ?: run {
    if (data.isWebUrl()) { "dweb://openinbrowser?url=$data" } else "dweb://search?q=$data"
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
//  deepLink.regexDeepLink()?.let { dwebLink ->
//    context.startActivity(Intent().apply {
//      `package` = context.packageName
//      action = Intent.ACTION_VIEW
//      this.data = Uri.parse(dwebLink)
//      addCategory("android.intent.category.BROWSABLE")
//      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//      putExtra("showBackground", showBackground)
//    })
//    return true
//  } ?: run {
//    Toast.makeText(
//      context, BrowserI18nResource.QRCode.toast_mismatching.text.format(data), Toast.LENGTH_SHORT
//    ).show()
//    return false
//  }
}