package org.dweb_browser.sys.scanning

actual fun getScanningController(): ScanningApi = ScanningAndroidController()

class ScanningAndroidController(): ScanningApi {
    override fun cameraPermission(): Boolean {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun recognize(img: ByteArray, rotation: Int): List<String> {
        TODO("Not yet implemented")
    }

}