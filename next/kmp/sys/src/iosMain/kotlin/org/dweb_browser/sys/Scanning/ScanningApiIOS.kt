package org.dweb_browser.sys.Scanning

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.CoreImage.*
import platform.posix.memcpy

actual fun getScanningController(): ScanningApi = ScanningIOSController()

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toImageBytes() : NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toImageBytes), length = this@toImageBytes.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray = ByteArray(this@toByteArray.length.toInt()).apply {
    usePinned {
        memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
}

class ScanningIOSController(): ScanningApi {
    override fun cameraPermission(): Boolean {
        // TODO: 权限暂时全部开放
        return true
    }

    override fun startScan(): String {
        println("mike startScan")
        return ""
    }

    override fun stopScan() {
        println("mike stopScan")
    }

    override fun recognize(img: ByteArray, rotation: Int): List<String> {
        println("mike recognize")
        val ciImage = CIImage(data = img.toImageBytes())

        val detector = CIDetector.detectorOfType("CIDetectorTypeQRCode", null, null)
        val detectResult = detector?.featuresInImage(image = ciImage, options = null) ?: return  emptyList()
        return (detectResult as List<CIQRCodeFeature>).map {
            println("Mike ${it.messageString}")
            it.messageString ?: ""
        }
    }
}