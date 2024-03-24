package org.dweb_browser.sys.scan

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.PurePoint
import org.dweb_browser.helper.PureRect

expect class ScanningManager() {
  fun stop()

  suspend fun recognize(img: ByteArray, rotation: Int): List<BarcodeResult>
}


@Serializable
class BarcodeResult(
  val data: String,
  val boundingBox: PureRect,
  val topLeft: PurePoint,
  val topRight: PurePoint,
  val bottomLeft: PurePoint,
  val bottomRight: PurePoint,
)