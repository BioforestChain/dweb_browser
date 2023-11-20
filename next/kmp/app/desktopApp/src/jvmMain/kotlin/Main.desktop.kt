import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.AutoSizeText
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.shared.Greeting
import org.dweb_browser.helper.platform.toImageBitmap


fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "Compose for Desktop",
    state = rememberWindowState(width = 300.dp, height = 300.dp)
  ) {
    val count = remember { mutableStateOf(0) }
    MaterialTheme {
      Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
        val imageBitmap =
          ("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".toBase64ByteArray()
            .toImageBitmap())
        Image(imageBitmap, contentDescription = null)
        Button(modifier = Modifier.align(Alignment.CenterHorizontally),
          onClick = {
            count.value++
          }) {
          Text(if (count.value == 0) "Hello World" else "Clicked ${count.value}!")
          Text("${Greeting().greet()}")
          AutoResizeTextContainer {
            AutoSizeText(text = "哈哈哈")
          }
        }
        Button(modifier = Modifier.align(Alignment.CenterHorizontally),
          onClick = {
            count.value = 0
          }) {
          Text("Reset")
        }
//        DesktopWebView(modifier = Modifier.fillMaxSize(), "https://www.baidu.com")
      }
    }
  }
}
