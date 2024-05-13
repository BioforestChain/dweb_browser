
import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.engine.Engine
import com.teamdev.jxbrowser.engine.EngineOptions
import com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.dweb_browser.dwebview.engine.window
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class WebViewEvalTest {
  suspend fun doTestEval(browser: Browser, useFix: Boolean) {
    val mainFrame = browser.mainFrame().get()
    val jsWindow = mainFrame.window()
    for (i in 1..100000) {
      println("test-$i")
      val key = java.util.UUID.randomUUID().toString()
      mainFrame.executeJavaScript<Unit>("window['$key']=()=>{return '$key'};void 0;")
      assertEquals(jsWindow.call<String>(key), key)
      jsWindow.removeProperty(key)
    }
  }

  @Test
  fun testEchoBug() = runTest(timeout = 600.seconds) {
    withContext(Dispatchers.Default) {
      val engine = Engine.newInstance(EngineOptions.newBuilder(OFF_SCREEN).run {
        this.licenseKey(System.getProperty("jxbrowser.license.key"))
        build()
      })
      val browser = engine.newBrowser()
      browser.devTools().show()
      val useFix = true
      doTestEval(browser, useFix)
      browser.close()
    }
  }
}