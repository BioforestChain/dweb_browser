package org.dweb_browser.dwebview.test

import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DWebViewDesktopTest {
  companion object {

    suspend fun getWebview(): DWebView {
      debugDWebView.forceEnable()
      val dwebview = DWebViewTest.getWebview()
      require(dwebview is DWebView)
      dwebview.viewEngine.browser.on(ConsoleMessageReceived::class.java) { event ->
        val consoleMessage = event.consoleMessage()
        val level = consoleMessage.level()
        val message = consoleMessage.message()
        println("TEST-JsConsole/$level | $message")
      }
      return dwebview
    }
  }

  @Test
  fun testMainFrame() = runCommonTest {
    val dwebview = getWebview()

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