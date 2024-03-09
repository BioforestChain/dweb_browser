package org.dweb_browser.dwebview.test

import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DWebViewTest {
  companion object {

    suspend inline fun getWebview(): IDWebView {
      val mm = object : NativeMicroModule("mm.test.dweb", "MM") {
        override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {}
        override suspend fun _shutdown() {}
      }
      return IDWebView.create(mm)
    }

  }

  @Test
  fun evalJavascript() = runCommonTest {
    val dwebview = getWebview()
    assertEquals("2", dwebview.evaluateAsyncJavascriptCode("1+1"))
    assertEquals("3", dwebview.evaluateAsyncJavascriptCode("await Promise.resolve(1)+2"))
    assertEquals("catch", runCatching {
      dwebview.evaluateAsyncJavascriptCode("await Promise.reject('no-catch')")
    }.getOrElse {
      assertEquals("no-catch", it.message)
      "catch"
    })

    dwebview.destroy()
  }

  @Test
  fun getUA() = runCommonTest {
    val dwebview = getWebview()
    dwebview.loadUrl("https://www.baidu.com")
    println(dwebview.evaluateAsyncJavascriptCode("JSON.stringify(navigator.userAgentData.brands)"))
    assertEquals(
      "true",
      dwebview.evaluateAsyncJavascriptCode("navigator.userAgentData.brands.filter((item)=>item.brand=='DwebBrowser').length>0")
    )
  }
}
