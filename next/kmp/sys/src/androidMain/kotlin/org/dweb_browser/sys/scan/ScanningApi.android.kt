package org.dweb_browser.sys.scan

import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.toJsonElement

class BarcodeResult(val data: ByteArray, val boundingBox: Rect, val cornerPoints: List<Point>)

actual class ScanningManager actual constructor() {
  actual fun cameraPermission(): Boolean {
    TODO("Not yet implemented")
  }

  actual fun stop() {
    BarcodeScanning.getClient().close()
  }

  actual suspend fun recognize(img: ByteArray, rotation: Int): List<String> {
    val image = InputImage.fromBitmap(BitmapFactory.decodeByteArray(img, 0, img.size), rotation)
    val result = mutableListOf<String>()
    process(image).forEach {
      result.add(it.toJsonElement().toString())
    }
    debugScanning("process", "result=> $result")
    return result
  }

  private suspend fun process(image: InputImage): List<BarcodeResult> {
    val task = PromiseOut<List<BarcodeResult>>()
    BarcodeScanning.getClient().process(image)
      .addOnSuccessListener { barcodes ->
        task.resolve(barcodes.map {
          BarcodeResult(
            it.rawBytes!!,
            it.boundingBox!!,
            it.cornerPoints!!.toList()
          )
        })
      }
      .addOnFailureListener { err ->
        task.reject(err)
      }
    return task.waitPromise()
  }
}