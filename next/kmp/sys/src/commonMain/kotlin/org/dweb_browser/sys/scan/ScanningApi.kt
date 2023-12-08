package org.dweb_browser.sys.scan

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.Point
import org.dweb_browser.helper.Rect

expect class ScanningManager() {

  fun cameraPermission(): Boolean

  fun stop()

  suspend fun recognize(img: ByteArray, rotation: Int): List<BarcodeResult>
}


@Serializable
class BarcodeResult(
  val data: String,
  val boundingBox: Rect,
  val topLeft: Point,
  val topRight: Point,
  val bottomLeft: Point,
  val bottomRight: Point,
)