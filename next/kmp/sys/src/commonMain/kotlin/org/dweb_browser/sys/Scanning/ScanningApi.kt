package org.dweb_browser.sys.Scanning

interface ScanningApi {

    //nativeFetch("file://permission.sys.dweb/query?permission=${EPermission.PERMISSION_CAMERA}").boolean()
    fun cameraPermission(): Boolean

    fun startScan(): String

    fun stopScan()

    fun recognize(img: ByteArray, rotation: Int): List<String>
}

expect fun getScanningController(): ScanningApi