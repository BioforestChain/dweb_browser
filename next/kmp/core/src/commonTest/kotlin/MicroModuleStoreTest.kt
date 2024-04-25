package info.bagen.dwebbrowser

import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.file.ext.store
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MicroModuleStoreTest {
  init {
    addDebugTags(listOf("/.+/"))
  }

  @Test
  fun testReadWrite() = runCommonTest {
    val dns = DnsNMM()
    val demoMM = TestMicroModule("demo.mm.dweb")
    dns.install(demoMM)
    dns.install(FileNMM())
    val dnsRuntime = dns.bootstrap()

    val demoRuntime = dnsRuntime.open(demoMM.mmid)
    val actual = randomUUID()
    demoRuntime.store.set("hi", actual)
    assertEquals(demoRuntime.store.get("hi"), actual)
  }
}