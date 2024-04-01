package org.dweb_browser.shared

import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.boot.BootNMM
import org.dweb_browser.sys.scan.ScanningNMM
import org.dweb_browser.sys.share.ShareNMM
import org.dweb_browser.sys.share.ext.postSystemShare

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
    bootNMM.postSystemShare(title = "Hello", text = "Contentxxxxxx")
  }

  suspend fun readyForTest(testNMM: NativeMicroModule.NativeRuntime): BootNMM {
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