package info.bagen.dwebbrowser

import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.sys.boot.BootNMM
import org.dweb_browser.sys.scanning.ScanningNMM
import org.dweb_browser.sys.share.ShareNMM

class TestEntry {

    suspend fun doScanningTest() {
        println("[iOS Test] doScanningTest")
        val bootNMM = readyForTest(ScanningNMM())
        val isYes = bootNMM.nativeFetch("file://barcode-scanning.sys.dweb/stop").boolean()
        println("[iOS Test] is $isYes")
    }

    suspend fun doShareTest() {
        println("[iOS Test] doShareTest")
        val bootNMM = readyForTest(ShareNMM())
//        val body = IPureBody()
        val request = PureRequest("file://share.sys.dweb/share?title=Hello&text=Contentxxxxxx", IpcMethod.POST)
        bootNMM.nativeFetch(request)
    }

    suspend fun readyForTest(testNMM: NativeMicroModule): BootNMM {
        val dnsNMM = DnsNMM()

        // 安装
        val scanNMM = ScanningNMM().also { dnsNMM.install(it) }
        val shareNMM = ShareNMM().also { dnsNMM.install(it) }

        /// 启动程序
        val bootNMM = BootNMM(
            listOf(
                scanNMM.mmid,
                shareNMM.mmid
            ),
        ).also { dnsNMM.install(it) }

        dnsNMM.bootstrap()

        return bootNMM
    }
}