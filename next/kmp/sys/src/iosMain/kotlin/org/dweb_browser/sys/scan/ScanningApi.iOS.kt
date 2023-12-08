package org.dweb_browser.sys.scan

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import org.dweb_browser.helper.toPoint
import org.dweb_browser.helper.toRect
import platform.CoreImage.CIDetector
import platform.CoreImage.CIImage
import platform.CoreImage.CIQRCodeFeature
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy


@OptIn(ExperimentalForeignApi::class)
public fun ByteArray.toNSData(): NSData = memScoped {
  NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
public fun NSData.toByteArray(): ByteArray = ByteArray(this@toByteArray.length.toInt()).apply {
  usePinned {
    memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
  }
}

actual class ScanningManager actual constructor() {
  actual fun cameraPermission(): Boolean {
    // TODO: 权限暂时全部开放
    return true
  }

  actual fun stop() {
    // iOS不需要stop
  }

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun recognize(img: ByteArray, rotation: Int): List<BarcodeResult> {
    val ciImage = CIImage(data = img.toNSData())
    val detector = CIDetector.detectorOfType("CIDetectorTypeQRCode", null, null)
    val detectResult =
      detector?.featuresInImage(image = ciImage, options = null) ?: return emptyList()
    @Suppress("UNCHECKED_CAST")
    return (detectResult as List<CIQRCodeFeature>).map {
      BarcodeResult(
        it.messageString ?: "",
        boundingBox = it.bounds.toRect(),
        topLeft = it.topLeft.toPoint(),
        topRight = it.topRight.toPoint(),
        bottomLeft = it.bottomLeft.toPoint(),
        bottomRight = it.bottomRight.toPoint(),
      )
    }
  }
}