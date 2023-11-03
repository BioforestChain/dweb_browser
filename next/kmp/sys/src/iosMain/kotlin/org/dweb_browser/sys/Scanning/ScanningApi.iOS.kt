package org.dweb_browser.sys.scanning

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import org.dweb_browser.sys.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.CoreImage.*
import platform.posix.memcpy

actual fun getScanningController(): ScanningApi = ScanningIOSController()

@OptIn(ExperimentalForeignApi::class)
public fun ByteArray.toNSData() : NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
public fun NSData.toByteArray(): ByteArray = ByteArray(this@toByteArray.length.toInt()).apply {
    usePinned {
        memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
}

class ScanningIOSController(): ScanningApi {
    override fun cameraPermission(): Boolean {
        // TODO: 权限暂时全部开放
        return true
    }

    override fun stop() {
        // iOS不需要stop
    }

    override fun recognize(img: ByteArray, rotation: Int): List<String> {
        val ciImage = CIImage(data = img.toNSData())
        val detector = CIDetector.detectorOfType("CIDetectorTypeQRCode", null, null)
        val detectResult = detector?.featuresInImage(image = ciImage, options = null) ?: return  emptyList()
        return (detectResult as List<CIQRCodeFeature>).map {
            it.messageString ?: ""
        }
    }
}