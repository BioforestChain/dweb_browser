package org.dweb_browser.sys.scan

import android.graphics.BitmapFactory
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.dweb_browser.helper.PurePoint
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.toPoint
import org.dweb_browser.helper.toRect

actual class ScanningManager actual constructor() {

  actual fun stop() {
    BarcodeScanning.getClient().close()
  }

  actual suspend fun recognize(img: ByteArray, rotation: Int): List<BarcodeResult> {
    val bit = BitmapFactory.decodeByteArray(img, 0, img.size) ?: return listOf()
    val image = InputImage.fromBitmap(bit, rotation)
    return process(image)
  }

  private suspend fun process(image: InputImage): List<BarcodeResult> {
    val task = PromiseOut<List<BarcodeResult>>()
    BarcodeScanning.getClient().process(image)
      .addOnSuccessListener { barcodes ->
        task.resolve(barcodes.map {
          val cornerPoints = it.cornerPoints;
          BarcodeResult(
            it.rawBytes?.decodeToString() ?: "",
            it.boundingBox?.toRect() ?: PureRect.Zero,
            topLeft = cornerPoints?.get(0)?.toPoint() ?: PurePoint.Zero,
            topRight = cornerPoints?.get(0)?.toPoint() ?: PurePoint.Zero,
            bottomLeft = cornerPoints?.get(0)?.toPoint() ?: PurePoint.Zero,
            bottomRight = cornerPoints?.get(0)?.toPoint() ?: PurePoint.Zero,
          )
        })
      }
      .addOnFailureListener { err ->
        task.reject(err)
      }
    return task.waitPromise()
  }
}