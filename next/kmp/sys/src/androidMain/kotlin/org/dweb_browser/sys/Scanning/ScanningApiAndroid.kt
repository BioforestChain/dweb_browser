package org.dweb_browser.sys.Scanning

//import org.dweb_browser.sys.Scanning.ScanningApi

actual fun getScanningController(): ScanningApi = ScanningAndroidController()

class ScanningAndroidController(): ScanningApi {
    override fun cameraPermission(): Boolean {
        TODO("Not yet implemented")
    }

    override fun startScan(): String {
        TODO("Not yet implemented")
    }

    override fun stopScan() {
        TODO("Not yet implemented")
    }

    override fun recognize(img: ByteArray, rotation: Int): List<String> {
        TODO("Not yet implemented")
    }

}