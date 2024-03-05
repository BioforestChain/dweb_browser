package org.dweb_browser.dwebview.test

import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DWebViewTest {
  @Test
  fun evalJavascript() = runCommonTest {
    val mm = object : NativeMicroModule("mm.test.dweb", "MM") {
      override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {}
      override suspend fun _shutdown() {}
    }
    val dwebview = IDWebView.create(mm)
    assertEquals("2", dwebview.evaluateAsyncJavascriptCode("1+1"))
    assertEquals("3", dwebview.evaluateAsyncJavascriptCode("await Promise.resolve(1)+2"))
    assertEquals("catch", runCatching {
      dwebview.evaluateAsyncJavascriptCode("await Promise.reject(2)")
    }.getOrElse {
      assertEquals("2", it.message)
      "catch"
    })
  }
}