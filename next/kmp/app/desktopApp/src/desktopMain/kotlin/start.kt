import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefJSDialogHandler
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.BoxLayout
import javax.swing.JPanel


fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "Compose for Desktop",
    state = rememberWindowState(width = 1400.dp, height = 850.dp)
  ) {
    val builder = CefAppBuilder()
//    builder.addJcefArgs("--enable-experimental-web-platform-features")
    builder.cefSettings.apply {
      user_agent = "DwebBrowser/2.0"
    }
    builder.setAppHandler(object : MavenCefAppHandlerAdapter() {
      override fun onBeforeTerminate(): Boolean {
        return super.onBeforeTerminate()
      }
    })
    val app = builder.build()
    val client = app.createClient()

    MaterialTheme {
      Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
        SwingBrowserView(client)
      }
    }
  }
}

@Composable
fun SwingBrowserView(
  client: CefClient
) {
  val jPanel = JPanel()
  val browser = client.createBrowser("https://www.baidu.com", false, false)
  val component = browser.uiComponent

  LaunchedEffect(Unit) {
    client.addLoadHandler(CefLoadHandlerImpl(jPanel))

    jPanel.apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      add(component)
    }
  }

  SwingPanel(
    modifier = Modifier.fillMaxSize(),
    factory = {
      jPanel
    }
  )
}

class CefLoadHandlerImpl(val jPanel: JPanel) : CefLoadHandlerAdapter() {
  override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
    browser?.devTools?.uiComponent.also {
      jPanel.add(it)
    }
    super.onLoadEnd(browser, frame, httpStatusCode)
  }
}