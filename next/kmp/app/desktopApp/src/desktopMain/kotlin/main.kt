import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.view.swing.BrowserView
import org.dweb_browser.platform.desktop.webview.WebviewEngine


fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "Compose for Desktop",
    state = rememberWindowState(width = 300.dp, height = 300.dp)
  ) {
    val dwebviewEngine = WebviewEngine.hardwareAccelerated {
      addSwitch("--enable-experimental-web-platform-features")
    }
    val browser = dwebviewEngine.newBrowser()
    MaterialTheme {
      Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
        SwingBrowserView(browser)
      }
    }
  }
}

@Composable
fun SwingBrowserView(
  browser: Browser,
  configure: (BrowserView) -> Unit = {}
) {
  val view = BrowserView.newInstance(browser)
  browser.navigation().loadUrl("https://www.baidu.com")
  browser.devTools().show()
  configure(view)
  SwingPanel(
    modifier = Modifier.fillMaxSize(),
    factory = { view }
  )
}