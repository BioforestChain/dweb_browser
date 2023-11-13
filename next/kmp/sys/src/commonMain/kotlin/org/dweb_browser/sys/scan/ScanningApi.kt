package org.dweb_browser.sys.scan

expect class ScanningManager() {

  fun cameraPermission(): Boolean

  fun stop()

  suspend fun recognize(img: ByteArray, rotation: Int): List<String>
}