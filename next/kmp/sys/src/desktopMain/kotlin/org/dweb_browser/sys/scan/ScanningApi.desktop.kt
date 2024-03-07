package org.dweb_browser.sys.scan

actual class ScanningManager actual constructor() {
  actual fun stop() {
  }

  // TODO 使用 openCV 来实现图像识别
  actual suspend fun recognize(
    img: ByteArray,
    rotation: Int
  ): List<BarcodeResult> {
    TODO("Not yet implemented recognize")
  }

}