package org.dweb_browser.dwebview.test

import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived
import com.teamdev.jxbrowser.navigation.event.FrameDocumentLoadFinished
import com.teamdev.jxbrowser.navigation.event.FrameLoadFailed
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished
import com.teamdev.jxbrowser.navigation.event.LoadFinished
import com.teamdev.jxbrowser.navigation.event.LoadProgressChanged
import com.teamdev.jxbrowser.navigation.event.LoadStarted
import com.teamdev.jxbrowser.navigation.event.NavigationFinished
import com.teamdev.jxbrowser.navigation.event.NavigationRedirected
import com.teamdev.jxbrowser.navigation.event.NavigationStarted
import com.teamdev.jxbrowser.navigation.event.NavigationStopped
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DWebViewDesktopTest {
  companion object {

    suspend fun getWebview(): DWebView {
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

    dwebview.viewEngine.loadUrl("data:image/png;base64,")
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
    dwebview.viewEngine.browser.navigation().apply {
      val evaluateSyncJavascriptFunctionBody =
        dwebview.viewEngine::evaluateSyncJavascriptFunctionBody;
      on(FrameDocumentLoadFinished::class.java) {
        println("QAQ FrameDocumentLoadFinished:${it.frame().browser().url()}")
        evaluateSyncJavascriptFunctionBody("(self.qaq||(self.qaq=[])).push('FrameDocumentLoadFinished');console.log(self.qaq)")
      }
      on(FrameLoadFailed::class.java) {
        println("QAQ FrameLoadFailed:${it.frame().browser().url()}")
        evaluateSyncJavascriptFunctionBody("(self.qaq||(self.qaq=[])).push('FrameLoadFailed');console.log(self.qaq)")
      }
      on(FrameLoadFinished::class.java) {
        println("QAQ FrameLoadFinished:${it.frame().browser().url()}")
        evaluateSyncJavascriptFunctionBody("(self.qaq||(self.qaq=[])).push('FrameLoadFinished');console.log(self.qaq)")
      }
      on(LoadFinished::class.java) {
        println("QAQ LoadFinished:${it.navigation().browser().url()}")
        evaluateSyncJavascriptFunctionBody("(self.qaq||(self.qaq=[])).push('LoadFinished');console.log(self.qaq)")
      }
      on(LoadProgressChanged::class.java) {
        println("QAQ LoadProgressChanged:${it.navigation().browser().url()}/${it.progress()}")
        evaluateSyncJavascriptFunctionBody("(self.qaq||(self.qaq=[])).push('LoadProgressChanged');console.log(self.qaq)")
      }
      on(LoadStarted::class.java) {
        println("QAQ LoadStarted:${it.navigation().browser().url()}")
        evaluateSyncJavascriptFunctionBody("(self.qaq||(self.qaq=[])).push('LoadStarted');console.log(self.qaq)")
      }
      on(NavigationFinished::class.java) {
        println("QAQ NavigationFinished:${it.url()}")
        evaluateSyncJavascriptFunctionBody("(self.qaq||(self.qaq=[])).push('NavigationFinished');console.log(self.qaq)")
      }
      on(NavigationRedirected::class.java) {
        println("QAQ NavigationRedirected:${it.destinationUrl()}")
        evaluateSyncJavascriptFunctionBody("(self.qaq||(self.qaq=[])).push('NavigationRedirected');console.log(self.qaq)")
      }
      on(NavigationStarted::class.java) {
        println("QAQ NavigationStarted:${it.url()}")
        evaluateSyncJavascriptFunctionBody("(self.qaq||(self.qaq=[])).push('NavigationStarted');console.log(self.qaq)")
      }
      on(NavigationStopped::class.java) {
        println("QAQ NavigationStopped:${it.navigation().browser().url()}")
        evaluateSyncJavascriptFunctionBody("(self.qaq||(self.qaq=[])).push('NavigationStopped');console.log(self.qaq)")
      }
      /// 这个不会重定向
      dwebview.loadUrl("https://www.baidu.com")
      /// 这个会重定向
      dwebview.loadUrl("https://baidu.com")

    }
    /// ----
  }
}