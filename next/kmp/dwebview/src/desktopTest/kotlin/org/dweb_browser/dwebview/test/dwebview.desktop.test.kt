package org.dweb_browser.dwebview.test

import kotlinx.coroutines.delay
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DWebViewDesktopTest {
  @Test
  fun testMainFrame() = runCommonTest {
    val mm = object : NativeMicroModule("mm.test.dweb", "MM") {
      override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {}
      override suspend fun _shutdown() {}
    }
    val dwebview = IDWebView.create(mm)
    require(dwebview is DWebView)

    val mainFrame1 = dwebview.viewEngine.mainFrame
    println("mainFrame1=$mainFrame1")
    println(dwebview.viewEngine.mainFrame.executeJavaScript<String>("location.href"))

    dwebview.viewEngine.loadUrl("data:image/png;base64,")
    val mainFrame2 = dwebview.viewEngine.mainFrame
    println("mainFrame2=$mainFrame2")
    println(dwebview.viewEngine.mainFrame.executeJavaScript<String>("location.href"))


    dwebview.loadUrl("https://baidu.com")
    val mainFrame3 = dwebview.viewEngine.mainFrame
    println("mainFrame3=$mainFrame3")
    println(dwebview.viewEngine.mainFrame.executeJavaScript<String>("location.href"))

    dwebview.viewEngine.browser.devTools()

    assertEquals(mainFrame1, mainFrame2)
    assertNotEquals(mainFrame3, mainFrame2)
    dwebview.destroy()
  }
}