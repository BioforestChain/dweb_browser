package org.dweb_browser.dwebview.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.asDesktop
import org.dweb_browser.dwebview.test.DWebViewTest.Companion.getWebview
import org.dweb_browser.test.runCommonTest
import kotlin.properties.Delegates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DWebViewDesktopTest {
  companion object {
    suspend fun getPrepareContext() = DWebViewTest.getPrepareContext()
    suspend fun getWebView(options: DWebViewOptions = DWebViewOptions()) =
      DWebViewTest.getWebview(options).asDesktop()
  }

  @Test
  fun testMainFrame() = runCommonTest {
    val dwebview = getWebView()

    val mainFrame1 = dwebview.viewEngine.mainFrame
    println("mainFrame1=$mainFrame1")
    println(dwebview.viewEngine.mainFrame.executeJavaScript<String>("location.href"))

    dwebview.loadUrl("data:image/png;base64,")
    val mainFrame2 = dwebview.viewEngine.mainFrame
    println("mainFrame2=$mainFrame2")
    println(dwebview.viewEngine.mainFrame.executeJavaScript<String>("location.href"))


    dwebview.loadUrl("https://baidu.com")
    val mainFrame3 = dwebview.viewEngine.mainFrame
    println("mainFrame3=$mainFrame3")
    println(dwebview.viewEngine.mainFrame.executeJavaScript<String>("location.href"))

    assertEquals(mainFrame1, mainFrame2)
    assertNotEquals(mainFrame3, mainFrame2)
    dwebview.destroy()
  }

  @Test
  fun testNavigationEvent() = runCommonTest {
    val dwebview = getWebview()
    /// 这个不会重定向
    dwebview.loadUrl("https://www.baidu.com")
    /// 这个会重定向
    dwebview.loadUrl("https://baidu.com")
  }
}