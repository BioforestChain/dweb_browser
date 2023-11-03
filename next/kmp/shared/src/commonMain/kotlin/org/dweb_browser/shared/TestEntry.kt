package org.dweb_browser.shared

import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.sys.boot.BootNMM
import org.dweb_browser.sys.clipboard.ClipboardNMM
import org.dweb_browser.sys.scanning.ScanningNMM

class TestEntry {
    suspend fun doScanningTest() {
        println("[iOS Test] doScanningTest")

        val bootNMM = readyForTest()

        val isYes = bootNMM.nativeFetch("file://barcode-scanning.sys.dweb/stop").boolean()


//        val imgByteArray = doTest() ?: return
//
//        val request = PureRequest("file://barcode-scanning.sys.dweb/process?rotation=1", IpcMethod.POST, body = IPureBody.from(imgByteArray))
//        val result = bootNMM.nativeFetch(request)
//
        println("[iOS Test] is $isYes")
    }

    suspend fun cliboardTest() {
        println("[iOS Test] doScanningTest")

        val bootNMM = readyForTest()

        val isYes = bootNMM.nativeFetch("file://clipboard.sys.dweb/write?string=test").boolean()

        val isYes22 = bootNMM.nativeFetch("file://clipboard.sys.dweb/read").toString()
//        val imgByteArray = doTest() ?: return
//
//        val request = PureRequest("file://barcode-scanning.sys.dweb/process?rotation=1", IpcMethod.POST, body = IPureBody.from(imgByteArray))
//        val result = bootNMM.nativeFetch(request)
//
        println("[iOS Test] is $isYes")

        println("[iOS Test] is $isYes22")
    }

    suspend fun readyForTest(): BootNMM {
        val dnsNMM = DnsNMM()

        // 安装
        val scanNMM = ScanningNMM().also { dnsNMM.install(it) }
        val clipboardNMM = ClipboardNMM().also { dnsNMM.install(it) }

        /// 启动程序
        val bootNMM = BootNMM(
            listOf(
                scanNMM.mmid,
                clipboardNMM.mmid
            ),
        ).also { dnsNMM.install(it) }

        dnsNMM.bootstrap()

        return bootNMM
    }
}